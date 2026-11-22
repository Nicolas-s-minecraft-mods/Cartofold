# Contributing

## Language

- Write all code, comments, documentation, commit messages, issues and pull requests in English.
- Use the style of ASD-STE100 (Simplified Technical English): short sentences, active voice, one instruction for each sentence, no semicolons.
- Documentation and comments describe the current state of the code and of the project. Do not refer to earlier versions, to the origin of a document or to the reason for a change. Put this information in commit messages and pull requests.
- The mod supports English (`en_us`) and French (`fr_fr`). Each text that the player sees has a key in the two files of `src/main/resources/assets/cartofold/lang/`. The build fails when a key is missing in one file (`checkTranslations`).

## Workflow

1. Open an issue, or select an existing issue.
2. Create a branch from `develop`. Use the format `<type>/<issue>-<short-name>`, for example `feat/12-minimap-hud`. Types: `feat`, `fix`, `docs`, `build`, `ci`, `refactor`, `test`, `chore`.
3. Make small commits. Use [Conventional Commits](https://www.conventionalcommits.org/), for example `feat(minimap): add the north arrow`. In a pull request with one commit, give the title of the commit the bump marker too: GitHub can use the title of the commit for the squash. Scopes: `engine`, `minimap`, `worldmap`, `paper`, `api`, `ci`, `docs`.
4. Open a pull request to `develop`. Write `Closes #<issue>` in the description. The title follows Conventional Commits and ends with a bump marker (see [Versions and releases](#versions-and-releases)), for example `feat(minimap): add the north arrow [minor]`.
5. The CI must pass before the merge. The check **Title and version** refuses a title with no bump marker, and its summary gives the version after the merge.
6. Use "Squash and merge": the title of the pull request becomes the title of the commit on `develop`. Delete the branch after the merge.

## Issues and project

The open issues of all the repositories of the organization are in the project [Mods project](https://github.com/orgs/Nicolas-s-minecraft-mods/projects/2). The workflow **Project** runs each hour and after each change of an issue of this repository. It adds the open issues to the project and reads these lines in the description of each issue:

- `Estimate: 6h`: the work that is necessary, in hours. The workflow sets the field **Estimate** of the project.
- `Blocked by: #12, Folkhold#597`: the issues that must close before this issue can start.
- `Blocks: Folkhold#597`: the issues that wait for this issue.

An issue is `#12` (same repository), `Folkhold#597` (same organization), `owner/repo#12` or the URL of the issue. The lines can be in a list or in bold. The workflow only adds: to remove a dependency or an item, use the issue or the project. The script is `scripts/project_sync.py`. The secret `PROJECT_TOKEN` of the organization gives the permissions of the script.

## Build and tests

- `./gradlew build`: compiles, checks the format (Spotless) and the translations, runs Error Prone and the unit tests (JUnit).
- `./gradlew spotlessApply`: fixes the format of the files.
- `./gradlew runGameTestServer`: runs the in-game tests (GameTest) in a new world, then stops.
- `./gradlew runClient` and `./gradlew runServer`: a client and a dedicated server with the mod.
- `mkdocs serve`: shows the documentation site. Install `requirements-docs.txt` first. Each new page goes in `nav` of `mkdocs.yml`.
- The pure logic (projection, colors, layers) has JUnit tests. The behavior in the game has GameTests.

## Versions and releases

- `develop` gets the pull requests. `main` gets only the merges of `develop`, and each merge makes a release.
- Each commit of `develop` has a bump marker at the end of its title:
  - `[patch]`: a fix or a small change. 1.2.3 becomes 1.2.4.
  - `[minor]`: a new feature. 1.2.3 becomes 1.3.0.
  - `[major]`: a change that breaks the worlds or the API. 1.2.3 becomes 2.0.0.
- The version starts from the last release tag (`vX.Y.Z`). The commits after the tag apply their bump in order, the oldest first. A commit with no marker keeps the version. A commit of Dependabot counts as `[patch]`.
- `python3 scripts/version.py version --rev <commit>` gives the version of a commit.
- The workflow **Build** has one job for each task: the version, the tests of the scripts, the build (compilation, format and translations), the unit tests and the GameTests. When all these jobs pass on `develop`, the last job publishes the dev build.
- The workflow **Build** gives each build a version: the version of the commit with `-dev.<commit>` for `develop`, and the version after the merge with `-pr.<number>.<commit>` for a pull request.
- The workflow **Release** runs on each push on `main`. It tags the commit of `develop` with `vX.Y.Z`, creates a GitHub Release with the jar and the release notes, publishes the jar to GitHub Packages, then publishes the jar, the sources jar and the Javadoc jar to Modrinth. It then sets the description and the icon of the Modrinth project from `docs/modrinth.md` and `src/main/resources/cartofold_logo.png`. The steps of Modrinth need the secret `MODRINTH_TOKEN` (scopes `VERSION_CREATE` and `PROJECT_WRITE`). When `gradle.properties` has `curseforge_project_id`, the workflow also publishes the jar to CurseForge, with the secret `CURSEFORGE_TOKEN` (an API token of CurseForge). With an empty value, it skips CurseForge. The workflow **CurseForge** ("Run workflow" with a version) sends an existing GitHub Release to CurseForge. Each push on `develop` also puts a dev build on Modrinth, in the channel `alpha`, with the version `X.Y.Z-dev.<commit>`. If the tag exists, it makes no release. To make a release, open a pull request from `develop` to `main` and merge it with a merge commit (not a squash), so that `develop` contains the tag.
