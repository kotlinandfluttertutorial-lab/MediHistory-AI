import os
os.environ['JWT_SECRET_KEY'] = 'test'

from app.main import app
from app.middleware.rate_limit import RateLimitMiddleware

# Force-build the middleware_stack
_ = app.middleware_stack

def find_rate_limiter(obj, depth=0, visited=None):
    if visited is None:
        visited = set()
    obj_id = id(obj)
    if obj_id in visited or depth > 15:
        return None
    visited.add(obj_id)
    
    if isinstance(obj, RateLimitMiddleware):
        return obj
    
    for attr in dir(obj):
        if attr.startswith('__'):
            continue
        try:
            val = getattr(obj, attr)
            if callable(val) and not isinstance(val, type):
                continue
            result = find_rate_limiter(val, depth+1, visited)
            if result is not None:
                print(f"Found via .{attr} at depth {depth+1}")
                return result
        except Exception:
            pass
    return None

result = find_rate_limiter(app.middleware_stack)
print(f"Result: {result}")

# Alternative: check if there's a __wrapped__ or __closure__
import inspect
stack = app.middleware_stack
print(f"\nMiddleware stack type: {type(stack)}")
print(f"Dir: {[x for x in dir(stack) if not x.startswith('_')]}")

# Look at closures
if hasattr(stack, '__wrapped__'):
    print(f"__wrapped__: {stack.__wrapped__}")
