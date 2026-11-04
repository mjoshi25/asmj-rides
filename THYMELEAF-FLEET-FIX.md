# Thymeleaf request-parameter condition fix

## Root cause
Thymeleaf's `param` expression object exposes request parameters as arrays of strings. Using a parameter array directly as a boolean (`th:if="${param.assigned or param.returnAssigned}"`) can fail SpringEL boolean conversion when the fleet dashboard renders.

## Fix
All template conditions that test for query-parameter flags now use explicit presence checks (`param.flag != null`). This includes fleet assignment success/error messages and equivalent status banners on admin, partner, driver, customer, login, payment, support, and location pages.

No controller routes, MongoDB models, booking data, or business logic were changed.
