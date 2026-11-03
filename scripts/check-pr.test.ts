import { describe, expect, test } from "bun:test";
import {
  validateBody,
  validateCommitSubject,
  validateCommits,
  validateTitle,
} from "./check-pr";

const VALID_BODY = `## Summary

Add the contributor guidance bootstrap.

## Related issue

Closes #42

## Type of change

- [x] Documentation

## What changed

- Added AGENTS.md with the commit convention.
- Added the pull request template.

## How tested

- [x] Not applicable (documentation/config-only change)

## Checklist

- [x] Commit messages follow the commit convention in \`AGENTS.md\`
- [x] Build passes, or the change does not affect Java code
- [x] Config/language/permissions/docs updated where applicable
- [x] Public API compatibility considered
- [x] No secrets, credentials, or server-specific data are committed
`;

describe("validateTitle", () => {
  test("accepts conventional titles", () => {
    expect(validateTitle("docs: add PR template")).toEqual([]);
    expect(validateTitle("fix(account): refresh cache")).toEqual([]);
    expect(validateTitle("feat(api)!: drop legacy getter")).toEqual([]);
  });

  test("rejects non-conventional titles", () => {
    expect(validateTitle("Add PR template").length).toBeGreaterThan(0);
    expect(validateTitle("docs: Add PR template").length).toBeGreaterThan(0);
    expect(validateTitle("docs add PR template").length).toBeGreaterThan(0);
  });

  test("rejects long titles", () => {
    const title = `docs: ${"a".repeat(80)}`;
    expect(validateTitle(title).join(" ")).toContain("72");
  });
});

describe("validateCommitSubject", () => {
  test("accepts conventional subjects", () => {
    expect(validateCommitSubject("fix(sql): use prepared statements")).toEqual([]);
    expect(validateCommitSubject("revert: feat(api): add setter")).toEqual([]);
  });

  test("ignores GitHub merge and revert commits", () => {
    expect(validateCommitSubject("Merge pull request #41 from branch")).toEqual([]);
    expect(validateCommitSubject('Revert "feat: add setter"')).toEqual([]);
  });

  test("rejects invalid subjects", () => {
    expect(validateCommitSubject("Bump gson").length).toBeGreaterThan(0);
  });
});

describe("validateCommits", () => {
  test("reports invalid subjects with the commit name", () => {
    const errors = validateCommits(["docs: okay", "update stuff"]);
    expect(errors).toHaveLength(1);
    expect(errors[0]).toContain("update stuff");
  });
});

describe("validateBody", () => {
  test("accepts the filled-in template", () => {
    expect(validateBody(VALID_BODY)).toEqual([]);
  });

  test("rejects an empty body", () => {
    expect(validateBody("")).toHaveLength(1);
    expect(validateBody(null)).toHaveLength(1);
  });

  test("reports every missing section", () => {
    const errors = validateBody("## Summary\n\nA sufficiently long summary.");
    expect(errors.join(" ")).toContain('"## Related issue"');
    expect(errors.join(" ")).toContain('"## Checklist"');
  });

  test("requires an issue reference", () => {
    const body = VALID_BODY.replace("Closes #42", "See the related work.");
    expect(validateBody(body).join(" ")).toContain("Closes #123");
  });

  test("requires a type of change", () => {
    const body = VALID_BODY.replace("- [x] Documentation", "- [ ] Documentation");
    expect(validateBody(body).join(" ")).toContain("Type of change");
  });

  test("requires a how-tested selection", () => {
    const body = VALID_BODY.replace(
      "- [x] Not applicable (documentation/config-only change)",
      "- [ ] Not applicable (documentation/config-only change)",
    );
    expect(validateBody(body).join(" ")).toContain("How tested");
  });

  test("requires every checklist item", () => {
    const body = VALID_BODY.replace(
      "- [x] Public API compatibility considered",
      "- [ ] Public API compatibility considered",
    );
    expect(validateBody(body).join(" ")).toContain("checklist items");
  });

  test("ignores HTML comments when looking for content", () => {
    const body = VALID_BODY.replace(
      "Add the contributor guidance bootstrap.",
      "<!-- fill this in -->",
    );
    expect(validateBody(body).join(" ")).toContain("Summary");
  });
});
