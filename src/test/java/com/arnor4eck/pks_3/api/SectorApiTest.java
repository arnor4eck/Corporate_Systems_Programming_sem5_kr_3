package com.arnor4eck.pks_3.api;

class SectorApiTest extends CrudApiTest {
    @Override protected String path() { return "/api/sectors"; }
    @Override protected String input() throws Exception {
        return """
                {"name":"Central"}
                """;
    }
    @Override protected String updatedInput() throws Exception {
        return """
                {"name":"Updated"}
                """;
    }
    @Override protected String changedField() { return "name"; }
    @Override protected Object changedValue() { return "Updated"; }
}
