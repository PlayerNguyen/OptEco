#!/usr/bin/env bun

/**
 * Validates a pull request's title, body, and commit subjects.
 *
 * CI runs this with GITHUB_EVENT_PATH set by GitHub Actions. For local runs:
 *   PR_TITLE="docs: update readme" PR_BODY="$(cat pr.md)" bun run check:pr
 *
 * Bypass: add the `skip-pr-check` label to the PR.
 */

const TYPES = [
  "feat",
  "fix",
  "docs",
  "style",
  "refactor",
  "perf",
  "test",
  "build",
  "ci",
  "chore",
  "revert",
] as const;

const TITLE_PATTERN = new RegExp(
  `^(${TYPES.join("|")})(\\([a-z0-9-]+\\))?!?: [a-z0-9].+$`,
);

const MAX_SUBJECT_LENGTH = 72;

const REQUIRED_SECTIONS = [
  "Summary",
  "Related issue",
  "Type of change",
  "What changed",
  "How tested",
  "Checklist",
] as const;

type PullRequestEvent = {
  action: string;
  pull_request: {
    number: number;
    title: string;
    body: string | null;
    draft: boolean;
    commits_url: string;
    user: { login: string; type: string };
    labels: Array<{ name: string }>;
  };
  repository: { full_name: string };
};

type GitHubCommit = {
  commit: { message: string };
};

function stripHtmlComments(value: string): string {
  return value.replace(/<!--[\s\S]*?-->/g, "");
}

function splitSections(body: string): Map<string, string> {
  const sections = new Map<string, string>();
  const lines = stripHtmlComments(body).replace(/\r\n/g, "\n").split("\n");
  let current: string | null = null;
  let buffer: string[] = [];

  for (const line of lines) {
    const heading = /^##\s+(.+?)\s*$/.exec(line);
    if (heading) {
      if (current) sections.set(current, buffer.join("\n").trim());
      current = heading[1].trim();
      buffer = [];
    } else if (current) {
      buffer.push(line);
    }
  }

  if (current) sections.set(current, buffer.join("\n").trim());
  return sections;
}

function countCheckboxes(section: string): { total: number; checked: number } {
  const matches = section.match(/^\s*[-*]\s+\[[ xX]\]/gm) ?? [];
  const checked = section.match(/^\s*[-*]\s+\[[xX]\]/gm) ?? [];
  return { total: matches.length, checked: checked.length };
}

export function validateTitle(title: string): string[] {
  const errors: string[] = [];

  if (title.trim().length === 0) {
    return ["PR title is empty."];
  }
  if (title.length > MAX_SUBJECT_LENGTH) {
    errors.push(
      `PR title is ${title.length} characters; keep it at ${MAX_SUBJECT_LENGTH} or fewer.`,
    );
  }
  if (!TITLE_PATTERN.test(title)) {
    errors.push(
      'PR title must follow Conventional Commits (see AGENTS.md#commit-convention), e.g. "docs: add PR template".',
    );
  }

  return errors;
}

export function validateCommitSubject(subject: string): string[] {
  const errors: string[] = [];

  // GitHub-generated merge and revert commits are exempt.
  if (/^Merge /i.test(subject) || /^Revert "/.test(subject)) {
    return errors;
  }
  if (subject.length > MAX_SUBJECT_LENGTH) {
    errors.push(
      `Commit subject is ${subject.length} characters; keep it at ${MAX_SUBJECT_LENGTH} or fewer.`,
    );
  }
  if (!TITLE_PATTERN.test(subject)) {
    errors.push(
      'Commit subject must follow Conventional Commits (see AGENTS.md#commit-convention).',
    );
  }

  return errors;
}

export function validateCommits(subjects: string[]): string[] {
  const errors: string[] = [];

  for (const subject of subjects) {
    for (const error of validateCommitSubject(subject)) {
      errors.push(`Commit "${subject}": ${error}`);
    }
  }

  return errors;
}

export function validateBody(body: string | null): string[] {
  if (!body || body.trim().length === 0) {
    return ["PR body is empty. Use the pull request template."];
  }

  const errors: string[] = [];
  const sections = splitSections(body);

  for (const name of REQUIRED_SECTIONS) {
    if (!sections.has(name)) {
      errors.push(`Missing required section: "## ${name}".`);
    }
  }

  const summary = sections.get("Summary");
  if (summary !== undefined && summary.length < 10) {
    errors.push('"## Summary" must contain a short description of the change.');
  }

  const related = sections.get("Related issue") ?? "";
  if (
    sections.has("Related issue") &&
    !/(?:closes|fixes|refs)\s+#\d+/i.test(related)
  ) {
    errors.push(
      '"## Related issue" must contain "Closes #123", "Fixes #123", or "Refs #123".',
    );
  }

  const typeOfChange = sections.get("Type of change");
  if (
    typeOfChange !== undefined &&
    countCheckboxes(typeOfChange).checked === 0
  ) {
    errors.push('Select at least one option under "## Type of change".');
  }

  const whatChanged = sections.get("What changed");
  if (whatChanged !== undefined && whatChanged.length < 5) {
    errors.push('"## What changed" must list the changes in this PR.');
  }

  const howTested = sections.get("How tested");
  if (howTested !== undefined && countCheckboxes(howTested).checked === 0) {
    errors.push('Check at least one option under "## How tested".');
  }

  const checklist = sections.get("Checklist");
  if (checklist !== undefined) {
    const boxes = countCheckboxes(checklist);
    if (boxes.total === 0) {
      errors.push('"## Checklist" must contain the pull request checklist.');
    } else if (boxes.checked !== boxes.total) {
      errors.push(
        `Complete all checklist items (${boxes.checked}/${boxes.total} checked).`,
      );
    }
  }

  return errors;
}

async function fetchCommitSubjects(commitsUrl: string): Promise<string[]> {
  const token = process.env.GITHUB_TOKEN;
  if (!token) {
    console.log("::warning title=PR check::GITHUB_TOKEN is not set; skipping commit subject checks.");
    return [];
  }

  const response = await fetch(commitsUrl, {
    headers: {
      Accept: "application/vnd.github+json",
      Authorization: `Bearer ${token}`,
      "X-GitHub-Api-Version": "2022-11-28",
    },
  });

  if (!response.ok) {
    throw new Error(`GitHub API returned ${response.status} for ${commitsUrl}`);
  }

  const commits = (await response.json()) as GitHubCommit[];
  return commits.map((commit) => commit.commit.message.split("\n")[0].trim());
}

async function writeSummary(
  prNumber: number | undefined,
  errors: string[],
  commitCount: number,
): Promise<void> {
  const summaryPath = process.env.GITHUB_STEP_SUMMARY;
  if (!summaryPath) return;

  const lines = [
    "## PR Check",
    "",
    prNumber ? `PR #${prNumber}` : "Local run",
    "",
  ];

  if (errors.length > 0) {
    lines.push("### Failed", "", ...errors.map((error) => `- ${error}`));
  } else {
    lines.push(
      "### Passed",
      "",
      "- PR title follows Conventional Commits.",
      "- PR body matches the template.",
      `- ${commitCount} commit subject(s) checked.`,
    );
  }

  await Bun.write(summaryPath, `${lines.join("\n")}\n`);
}

export async function main(): Promise<number> {
  const eventPath = process.env.GITHUB_EVENT_PATH;
  const event = eventPath
    ? ((await Bun.file(eventPath).json()) as PullRequestEvent)
    : undefined;

  let title = process.env.PR_TITLE ?? "";
  let body = process.env.PR_BODY ?? "";
  let commitSubjects: string[] = [];
  let prNumber: number | undefined;

  if (event) {
    const pr = event.pull_request;
    prNumber = pr.number;

    if (pr.draft) {
      console.log(`PR #${pr.number} is a draft; skipping checks.`);
      return 0;
    }
    if (pr.user?.type === "Bot") {
      console.log(`PR #${pr.number} was opened by a bot; skipping checks.`);
      return 0;
    }
    if (pr.labels?.some((label) => label.name === "skip-pr-check")) {
      console.log(`PR #${pr.number} has the skip-pr-check label; skipping checks.`);
      return 0;
    }

    title = pr.title;
    body = pr.body ?? "";

    try {
      commitSubjects = await fetchCommitSubjects(pr.commits_url);
    } catch (error) {
      console.log(
        `::warning title=PR check::Could not read PR commits: ${(error as Error).message}`,
      );
    }
  }

  if (!title) {
    console.error(
      "No PR title available. Set GITHUB_EVENT_PATH (CI) or PR_TITLE (local).",
    );
    return 1;
  }

  if (!event && process.env.PR_COMMITS) {
    commitSubjects = process.env.PR_COMMITS.split("\n").filter(Boolean);
  }

  const errors = [
    ...validateTitle(title),
    ...validateBody(body || null),
    ...validateCommits(commitSubjects),
  ];

  await writeSummary(prNumber, errors, commitSubjects.length);

  if (errors.length > 0) {
    for (const error of errors) {
      console.log(`::error title=PR check::${error}`);
    }
    console.error(`\nPR check failed with ${errors.length} error(s).`);
    return 1;
  }

  console.log(
    `PR check passed${prNumber ? ` for #${prNumber}` : ""} (${commitSubjects.length} commit(s) checked).`,
  );
  return 0;
}

if (import.meta.main) {
  process.exitCode = await main();
}
