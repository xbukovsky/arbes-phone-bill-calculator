import com.phonecompany.billing.TelephoneBillCalculatorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Some basics telephone bill calculator impl tests
public class TelephoneBillCalculatorTests {
    TelephoneBillCalculatorImpl billCalculator;

    @BeforeEach
    void init() {
        this.billCalculator = new TelephoneBillCalculatorImpl();
    }

    @Test
    void testOneShortCallOutOfMainInterval() {
        String line = "420774577453,13-01-2020 18:10:15,13-01-2020 18:10:16";
        BigDecimal sum = this.billCalculator.calculate(line);
        assertEquals(sum.doubleValue(), 0.5);
        assertTrue(sum.doubleValue() < 1.0);
    }

    @Test
    void testCallDuringTwoDays() {
        String line = "420776562353,19-01-2020 08:59:20,20-01-2020 09:10:00"; //294.2
        BigDecimal sum = this.billCalculator.calculate(line);
        assertEquals(sum.doubleValue(), 294.2);
    }

    @Test
    @DisplayName("Test with file reading")
    void testMostFrequentlyUsedTelephone() {
        String[] args = new String[] {"calculator.csv"};
        String line;
        BigDecimal sum = BigDecimal.valueOf(0.0);

        try {
            BufferedReader br = new BufferedReader(new FileReader(args[0]));
            while ((line = br.readLine()) != null) {
                sum = sum.add(this.billCalculator.calculate(line));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertEquals(this.billCalculator.getPhoneWithHighestNumberOfCalls(), "420776562352");
        assertEquals(sum, BigDecimal.valueOf(630.5));
    }

    @Test
    @DisplayName("Test list of telephone numbers")
    void testPrintOfExistingTelephoneNumbers() {
        String[] args = new String[] {"calculator.csv"};
        String line;

        try {
            BufferedReader br = new BufferedReader(new FileReader(args[0]));
            while ((line = br.readLine()) != null) {
                this.billCalculator.calculate(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<String> list = new ArrayList<>(this.billCalculator.getListOfTelephones().keySet());

        assertEquals(list, new ArrayList<> (List.of("420774577453", "420776562353", "420776562352")));
    }
}
