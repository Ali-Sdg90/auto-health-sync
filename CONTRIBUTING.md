# Contributing

Contributions are welcome when they keep Auto Health Sync small, reliable, and privacy-focused.

1. Search existing issues before starting substantial work.
2. Create a focused branch and keep changes scoped.
3. Add or update tests for behavioral changes.
4. Run:

   ```shell
   ./gradlew testDebugUnitTest lintDebug assembleDebug
   ```

5. Open a pull request with a Conventional Commit title, such as `fix(backup): handle expired authorization` or `feat(settings): select backup fields`.

Do not include credentials, signing material, access tokens, or personal health data. Product behavior should remain consistent with [vision.md](vision.md).
