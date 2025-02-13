-- Create role table
CREATE TABLE role (
                      id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                      role TEXT NOT NULL
);

-- Create staff_role table (Many-to-Many relationship between staff and role)
CREATE TABLE staff_role (
                            users_staff_id INTEGER NOT NULL,
                            roles_id BIGINT NOT NULL,
                            PRIMARY KEY (users_staff_id, roles_id),
                            FOREIGN KEY (users_staff_id) REFERENCES staff (staff_id),
                            FOREIGN KEY (roles_id) REFERENCES role (id)
);

INSERT INTO role (role) VALUES ('user');
INSERT INTO role (role) VALUES ('staff');

INSERT INTO staff_role (users_staff_id, roles_id) VALUES (1, 2); -- Associate role to user
INSERT INTO staff_role (users_staff_id, roles_id) VALUES (2, 1);
