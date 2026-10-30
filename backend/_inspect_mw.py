import os
os.environ['JWT_SECRET_KEY'] = 'test'

from app.main import app
from app.middleware.rate_limit import RateLimitMiddleware

m = app.user_middleware[0]
print(type(m))
print('cls:', m.cls)
print('kwargs:', m.kwargs)

# Force-build the middleware_stack
_ = app.middleware_stack
stack = app.middleware_stack
depth = 0
while stack is not None and depth < 30:
    name = type(stack).__name__
    print(f'{depth}: {name}', end='')
    if isinstance(stack, RateLimitMiddleware):
        print(' <-- FOUND')
        break
    print()
    # Check for common wrapper patterns
    for attr in ['app', 'handler', '_app', 'inner']:
        val = getattr(stack, attr, None)
        if val is not None and val is not stack:
            print(f'  via .{attr}')
    stack = getattr(stack, 'app', None)
    depth += 1
else:
    print('NOT FOUND')
