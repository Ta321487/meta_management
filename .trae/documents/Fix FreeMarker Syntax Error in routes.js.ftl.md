**Issue**: In `routes.js.ftl` at line 10, column 43, there's an invalid FreeMarker syntax using `?node` which is not a valid built-in.

**Root Cause**: The template is using incorrect conditional syntax. The line `${node.routePath?has_content?node.routePath:node.jumpRelation}` is trying to use `?node` as a built-in, which doesn't exist in FreeMarker 2.3.32.

**Fix**: Replace the invalid syntax with the correct FreeMarker conditional syntax using the `?then()` built-in:
```freemarker
path: '${node.routePath?has_content?then(node.routePath, node.jumpRelation)}',
```

**Expected Result**: The template will compile successfully, and the generated routes.js file will correctly use `routePath` when available, otherwise `jumpRelation`.