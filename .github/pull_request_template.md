## Summary

Describe what changed and why.

## Validation

- [ ] `./gradlew testDebugUnitTest lintDebug assembleDebug` passes locally
- [ ] Relevant behavior was tested on a physical Android device, when applicable
- [ ] No signing keys, credentials, tokens, or personal health data are included

## Release impact

Use a Conventional Commit PR title because squash merges use it for versioning:

- `fix(scope): ...` for a patch release
- `feat(scope): ...` for a minor release
- `feat(scope)!: ...` for a breaking major release
- `docs:`, `test:`, `ci:`, `build:`, `refactor:`, or `chore:` when no release is needed
