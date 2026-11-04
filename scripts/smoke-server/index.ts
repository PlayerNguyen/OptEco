#!/usr/bin/env bun

/**
 * Boots a real Purpur server headlessly, installs the built OptEco jar, drives
 * the console with a few commands, and fails when the run is not clean.
 *
 * This is the packaging and loader smoke test: it catches the problems
 * MockBukkit cannot see, such as a broken jar, an incompatible server API, a
 * bad plugin.yml or a startup failure in the real plugin class loader.
 *
 * Usage:
 *   ./gradlew jar
 *   bun run smoke:server
 *
 * Environment:
 *   MC_VERSION        Purpur/Minecraft version to run (default: 1.20.4)
 *   SERVER_DIR        Working directory for the server (default: build/smoke-server)
 *   STARTUP_TIMEOUT   Seconds to wait for "Done (" (default: 240)
 *   COMMAND_TIMEOUT   Seconds to wait for a console command answer (default: 60)
 *
 * CI runs this from .github/workflows/server-smoke.yml on ubuntu-latest, but it
 * also runs on Windows and macOS because the console is driven through a pipe
 * instead of a POSIX FIFO.
 */

import {
  appendFileSync,
  copyFileSync,
  existsSync,
  mkdirSync,
  readdirSync,
  rmSync,
  writeFileSync,
} from "node:fs";
import { basename, join, relative, resolve } from "node:path";

const ROOT = resolve(import.meta.dir, "../..");

const ERROR_PATTERN =
  /SEVERE|Could not load|Failed to load|Error occurred|Unhandled exception/;

/** Paper colourises console output; strip it before matching log lines. */
const ANSI_PATTERN = /\x1b\[[0-9;]*m/g;

const SERVER_PROPERTIES = `online-mode=false
level-type=minecraft:flat
generate-structures=false
spawn-protection=0
max-players=1
view-distance=4
simulation-distance=4
allow-nether=false
enable-command-block=false
sync-chunk-writes=false
terminal.ansi=false
motd=OptEco smoke test
`;

/** Thrown for an expected smoke-test failure; the message is user-facing. */
class SmokeFailure extends Error {}

type Config = {
  minecraftVersion: string;
  serverDir: string;
  logFile: string;
  startupTimeoutMs: number;
  commandTimeoutMs: number;
};

type RunningServer = {
  pid: number;
  exited: () => boolean;
  send: (command: string) => Promise<void>;
  waitFor: (pattern: string, description: string, timeoutMs: number) => Promise<void>;
  waitForExit: () => Promise<number>;
  firstError: () => string | undefined;
  kill: () => void;
  tail: (lineCount: number) => string;
};

function log(message: string): void {
  console.log(`[smoke] ${message}`);
}

function sleep(milliseconds: number): Promise<void> {
  return new Promise((resolveSleep) => setTimeout(resolveSleep, milliseconds));
}

function env(name: string, fallback: string): string {
  const value = process.env[name];
  return value === undefined || value.trim() === "" ? fallback : value.trim();
}

function seconds(name: string, fallback: string): number {
  const raw = env(name, fallback);
  const parsed = Number(raw);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    throw new SmokeFailure(`${name} must be a positive number of seconds, got "${raw}"`);
  }
  return Math.round(parsed * 1000);
}

function loadConfig(): Config {
  const serverDir = resolve(ROOT, env("SERVER_DIR", "build/smoke-server"));
  return {
    minecraftVersion: env("MC_VERSION", "1.20.4"),
    serverDir,
    logFile: join(serverDir, "server.log"),
    startupTimeoutMs: seconds("STARTUP_TIMEOUT", "240"),
    commandTimeoutMs: seconds("COMMAND_TIMEOUT", "60"),
  };
}

/** Picks the plugin jar produced by `./gradlew jar`. */
function findPluginJar(): string {
  const libs = join(ROOT, "build/libs");
  if (!existsSync(libs)) {
    throw new SmokeFailure("build/libs is missing; run ./gradlew jar first");
  }
  const jar = readdirSync(libs)
    .filter((name) => name.endsWith(".jar"))
    .filter((name) => !name.endsWith("-sources.jar") && !name.endsWith("-javadoc.jar"))
    .sort()[0];
  if (jar === undefined) {
    throw new SmokeFailure("no plugin jar in build/libs; run ./gradlew jar first");
  }
  return join(libs, jar);
}

/**
 * Reads plugin.yml from the built jar and rejects an unreplaced version token.
 * Falls back to the processed resource when `unzip` is unavailable.
 */
async function readPluginManifest(jar: string): Promise<string> {
  try {
    const result = Bun.spawnSync(["unzip", "-p", jar, "plugin.yml"], {
      stdout: "pipe",
      stderr: "pipe",
    });
    if (result.exitCode === 0) {
      return result.stdout.toString();
    }
  } catch {
    // `unzip` is missing on this machine; use the processed resource instead.
  }
  const resource = join(ROOT, "build/resources/main/plugin.yml");
  if (!existsSync(resource)) {
    throw new SmokeFailure("cannot read plugin.yml from the jar or build/resources/main");
  }
  return Bun.file(resource).text();
}

function prepareServer(config: Config, pluginJar: string): void {
  rmSync(config.serverDir, { recursive: true, force: true });
  mkdirSync(join(config.serverDir, "plugins/OptEco"), { recursive: true });
  mkdirSync(join(config.serverDir, "plugins/bStats"), { recursive: true });

  writeFileSync(join(config.serverDir, "eula.txt"), "eula=true\n");
  writeFileSync(join(config.serverDir, "server.properties"), SERVER_PROPERTIES);
  // Keep the run offline and deterministic: no update check, no bStats.
  writeFileSync(
    join(config.serverDir, "plugins/OptEco/config.yml"),
    "Update:\n  CheckForUpdate: false\n",
  );
  writeFileSync(
    join(config.serverDir, "plugins/bStats/config.yml"),
    "enabled: false\nserverUuid: 00000000-0000-0000-0000-000000000000\n",
  );
  copyFileSync(pluginJar, join(config.serverDir, "plugins", basename(pluginJar)));
}

/** Downloads the server jar once and caches it under build/cache. */
async function downloadServer(config: Config): Promise<void> {
  const cacheDir = join(ROOT, "build/cache");
  const cached = join(cacheDir, `purpur-${config.minecraftVersion}.jar`);
  const target = join(config.serverDir, "purpur.jar");
  if (!existsSync(cached)) {
    mkdirSync(cacheDir, { recursive: true });
    log(`downloading Purpur ${config.minecraftVersion}`);
    const url = `https://api.purpurmc.org/v2/purpur/${config.minecraftVersion}/latest/download`;
    const response = await fetch(url);
    if (!response.ok) {
      throw new SmokeFailure(`could not download Purpur ${config.minecraftVersion}: HTTP ${response.status}`);
    }
    await Bun.write(cached, response);
  }
  copyFileSync(cached, target);
}

/** Starts the server and exposes its console through a writable pipe. */
function startServer(config: Config): RunningServer {
  const lines: string[] = [];
  const errors: string[] = [];
  let partial = "";
  const decoder = new TextDecoder();

  const record = (chunk: Uint8Array): void => {
    const text = decoder.decode(chunk, { stream: true });
    appendFileSync(config.logFile, text);
    partial += text.replace(ANSI_PATTERN, "").replace(/\r/g, "");
    let newline = partial.indexOf("\n");
    while (newline >= 0) {
      const line = partial.slice(0, newline);
      lines.push(line);
      if (ERROR_PATTERN.test(line)) {
        errors.push(line);
      }
      partial = partial.slice(newline + 1);
      newline = partial.indexOf("\n");
    }
    if (lines.length > 4000) {
      lines.splice(0, lines.length - 4000);
    }
  };

  const server = Bun.spawn(["java", "-Xms1G", "-Xmx2G", "-jar", "purpur.jar", "--nogui"], {
    cwd: config.serverDir,
    stdin: "pipe",
    stdout: "pipe",
    stderr: "pipe",
  });

  let exited = false;
  let exitCode: number | null = null;
  const exitedPromise = server.exited.then((code) => {
    exited = true;
    exitCode = code;
    return code;
  });

  void pump(server.stdout as ReadableStream<Uint8Array>, record);
  void pump(server.stderr as ReadableStream<Uint8Array>, record);

  const waitFor = async (
    pattern: string,
    description: string,
    timeoutMs: number,
  ): Promise<void> => {
    const deadline = Date.now() + timeoutMs;
    while (Date.now() < deadline) {
      if (lines.some((line) => line.includes(pattern))) {
        log(`ok: ${description}`);
        return;
      }
      if (exited) {
        throw new SmokeFailure(`server exited with status ${exitCode} while waiting for ${description}`);
      }
      await sleep(200);
    }
    throw new SmokeFailure(`timed out after ${timeoutMs / 1000}s waiting for ${description}`);
  };

  return {
    pid: server.pid,
    exited: () => exited,
    send: async (command: string) => {
      log(`console> ${command}`);
      server.stdin.write(`${command}\n`);
      await server.stdin.flush();
    },
    waitFor,
    waitForExit: () => exitedPromise,
    firstError: () => errors[0],
    kill: () => {
      server.kill();
    },
    tail: (lineCount: number) => lines.slice(-lineCount).join("\n"),
  };
}

async function pump(
  stream: ReadableStream<Uint8Array>,
  onChunk: (chunk: Uint8Array) => void,
): Promise<void> {
  try {
    for await (const chunk of stream) {
      onChunk(chunk);
    }
  } catch {
    // The stream closes when the server exits; nothing to report here.
  }
}

/** Rejects when the promise takes longer than the timeout. */
function withTimeout<T>(promise: Promise<T>, timeoutMs: number, description: string): Promise<T> {
  const timeout = sleep(timeoutMs).then(() => {
    throw new SmokeFailure(`timed out after ${timeoutMs / 1000}s waiting for ${description}`);
  });
  return Promise.race([promise, timeout]);
}

async function driveConsole(config: Config, server: RunningServer): Promise<void> {
  await server.waitFor("Done (", "the server to finish starting", config.startupTimeoutMs);
  await server.waitFor(
    "OptEco enabled with SQLite",
    "OptEco to enable against SQLite",
    config.commandTimeoutMs,
  );

  const created = ["plugins/OptEco/account.sqlite", "plugins/OptEco/language/en.yml"];
  for (const path of created) {
    if (!existsSync(join(config.serverDir, path))) {
      throw new SmokeFailure(`OptEco did not create ${path}`);
    }
  }
  log("ok: OptEco created its database and language file");

  const steps: Array<{ command: string; expected: string; description: string }> = [
    { command: "points top", expected: "No accounts yet.", description: "the empty leaderboard" },
    {
      command: "points me",
      expected: "This command can only be used by a player.",
      description: "the player-only rejection",
    },
    {
      command: "points add Alice 100",
      expected: "Player Alice was not found.",
      description: "the missing-account error path",
    },
    {
      command: "points reload",
      expected: "Configuration and language reloaded.",
      description: "the configuration reload",
    },
    { command: "points nope", expected: "Command not found.", description: "the unknown subcommand error" },
    { command: "points help", expected: "OptEco Commands", description: "the help listing" },
  ];

  for (const step of steps) {
    await server.send(step.command);
    await server.waitFor(step.expected, step.description, config.commandTimeoutMs);
  }

  await server.send("stop");
  const exitCode = await withTimeout(
    server.waitForExit(),
    config.commandTimeoutMs,
    "the server to stop",
  );
  if (exitCode !== 0) {
    throw new SmokeFailure(`the server exited with status ${exitCode}`);
  }
  log("ok: the server stopped cleanly");
}

async function main(): Promise<void> {
  const config = loadConfig();
  const pluginJar = findPluginJar();
  log(`plugin jar: ${relative(ROOT, pluginJar)}`);

  const manifest = await readPluginManifest(pluginJar);
  if (manifest.includes("@VERSION@")) {
    throw new SmokeFailure("plugin.yml still contains the unreplaced @VERSION@ token");
  }
  const version = /^version:\s*(.+)$/m.exec(manifest)?.[1]?.trim() ?? "unknown";
  log(`ok: plugin.yml version ${version}`);

  prepareServer(config, pluginJar);
  await downloadServer(config);

  const server = startServer(config);
  activeServer = server;
  try {
    await driveConsole(config, server);
  } finally {
    if (!server.exited()) {
      server.kill();
    }
  }

  if (/SEVERE|Could not load|Failed to load|Error occurred|Unhandled exception/.test(manifest)) {
    throw new SmokeFailure("unreachable");
  }
  const offending = server.firstError();
  if (offending !== undefined) {
    throw new SmokeFailure(`the server log contains an error: ${offending}`);
  }

  log(
    `PASS: Purpur ${config.minecraftVersion} booted, OptEco enabled, console commands answered and the server stopped cleanly`,
  );
}

/** Server used for the failure log tail, if one was started. */
let activeServer: RunningServer | null = null;

if (import.meta.main) {
  try {
    await main();
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    console.error(`::error::${message}`);
    const tail = activeServer?.tail(120);
    if (tail !== undefined && tail !== "") {
      console.error("--- last 120 server log lines ---");
      console.error(tail);
    }
    process.exit(1);
  }
}
