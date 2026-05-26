insert into task (name, status, created_at) values
    ('task-alpha',   'PENDING',   now() - interval '10 days'),
    ('task-beta',    'PENDING',   now() - interval '5 days'),
    ('task-gamma',   'COMPLETED', now() - interval '3 days'),
    ('task-delta',   'STALE',     now() - interval '30 days'),
    ('task-epsilon', 'STALE',     now() - interval '60 days');
