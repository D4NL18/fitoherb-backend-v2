# Fix CORS QA Domain

- [ ] Modify `SecurityConfigurations.java` to add `https://qa.fitoherb.com.br` to allowed origins.
- [ ] Modify `WebConfig.java` to add `https://qa.fitoherb.com.br` (and maybe others like `https://fitoherb.com.br` to match `SecurityConfigurations.java`) to allowed origins.
- [ ] Create Pull Request to `develop`.
