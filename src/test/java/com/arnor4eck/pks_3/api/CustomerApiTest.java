package com.arnor4eck.pks_3.api;

class CustomerApiTest extends CrudApiTest {
    @Override protected String path() { return "/api/customers"; }
    @Override protected String input() throws Exception {
        return """
                {"fullName":"Customer","phone":"+71111111111","email":"user@example.com"}
                """;
    }
    @Override protected String updatedInput() throws Exception {
        return """
                {"fullName":"Updated","phone":"+72222222222","email":null}
                """;
    }
    @Override protected String changedField() { return "fullName"; }
    @Override protected Object changedValue() { return "Updated"; }
}
