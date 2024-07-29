package org.sat4j;

import org.junit.Test;
import org.sat4j.fuzzer.APIFuzzer;

public class TestAPIGenerator {

    @Test
    public void runAPIFuzzerForCoverage() {
        APIFuzzer.main(new String[] {"-n", "50", "-p"});
    }
}
