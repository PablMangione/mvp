alter table enrollment
    change enrollment_date created_at timestamp default CURRENT_TIMESTAMP null;
alter table group_request
    change request_date created_at timestamp default CURRENT_TIMESTAMP null;