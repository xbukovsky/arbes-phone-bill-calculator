package com.phonecompany.billing;

import java.math.BigDecimal;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

// Vstup metody bude splnovat nize uvedena kriteria
/*
        Vstupem metody je textový řetězec obsahující výpis volání. Výpis volání je ve formátu CSV s následujícími poli:
        • Telefonní číslo v normalizovaném tvaru obsahující pouze čísla (např. 420774567453)
        • Začátek hovoru ve tvaru dd-MM-yyyy HH:mm:ss
        • Konec hovoru ve tvaru dd-MM-yyyy HH:mm:ss

        Příklad obsahu souboru:
        420774577453,13-01-2020 18:10:15,13-01-2020 18:12:57 --> 3 minuty po 0.5 --> 1.5
        420776562353,18-01-2020 08:59:20,18-01-2020 09:01:00 --> 2 minuta po 1 --> 2
        420776562353,19-01-2020 08:59:20,20-01-2020 09:10:00 --> SILENA SUMA, jsou to rozdilne dny! (5 minut po 1 + 1446 minut po 0.2 --> 294.2)
        420776562353,19-01-2020 08:59:20,19-01-2020 09:10:00 --> 5 minut po 1 + 6 minut po 0.2 --> 6.2
        420776562353,27-01-2020 04:59:20,27-01-2020 05:10:00 --> 5 minut po 0.5 + 6 minut po 0.2 --> 2.5 + 1.2 = 3.7
        420776562353,27-01-2020 07:57:25,27-01-2020 08:03:42 --> 3 minuty po 0.5 + 2 minuty po 1 + 2 minuty po 0.2 --> 3.9
        420776562353,27-01-2020 15:57:25,27-01-2020 16:01:42 --> 3 minuty po 1 + 2 minuty po 0.5 --> 4.0

        BEGIN 07:57:25 with END 08:03:42 --> 7 minutes
        OR BEGIN 15:57:25 with END 16:03:42 --> 7 minutes

        Výstupem metody je částka k uhrazení spočtena dle vstupního výpisu dle následujících pravidel:
    • Minutová sazba v intervalu <8:00:00,16:00:00) je zpoplatněna 1 Kč za každou započatou minutu.
    Mimo uvedený interval platí snížená sazba 0,50 Kč za každou započatou minutu.
    Pro každou minutu hovoru je pro stanovení sazby určující čas započetí dané minuty.

    • Pro hovory delší, než pět minut platí pro každou další započatou minutu nad rámec prvních pěti minut snížená sazba 0,20 Kč
    bez ohledu na dobu kdy telefonní hovor probíhá.

    • V rámci promo akce operátora dále platí, že hovory na nejčastěji volané číslo v rámci výpisu nebudou zpoplatněny.
    V případě, že by výpis obsahoval dvě nebo více takových čísel, zpoplatněny nebudou hovory na číslo s aritmeticky nejvyšší hodnotou.

    Zde me napada reseni ukladani telefonnich cisel do databaze, ziskavani pomoci getByPhoneNumber, aktualizace jeho plateb a ukladani poctu vyskytu v CSV souboru.
    Jednodussi by to ale v tomto pripade bylo asi resit pres some multidimensional associative array --> {+420777666555 -> {numOfCalls: 6, summary: 24.5}, +420777555666 -> {numOfCalls: 3, summary: 12.2}}
    Java asociativni pole nepodporuje

    JAK ZDE chapat aritmeticky nejvyssi hodnotu? Aritmeticky nejvyssi hodnotu samotneho telefonniho cisla, nebo poctu volani vuci sazbe za hovory? Prvotne pracovano s telefonnim cislem, pote zmeneno na pocet hovoru vuci castce za hovor

    Výsledek vaší práce umístěte do repository ve některém z veřejných úložišť zdrojových kódů (např. GitHub) a zašlete nám na něj odkaz.
    Repository by mělo obsahovat veškeré zdrojové kódy a pomocné soubory pro sestavení technického modulu plus automatické testy.
 */

public class TelephoneBillCalculatorImpl implements TelephoneBillCalculator {
    public static final double BASIC_CALL_TAX = 1;
    public static final double OUT_OF_BASIC_INTERVAL_CALL_TAX = 0.5;
    public static final double MORE_THAN_FIVE_MINUTES_CALL_TAX = 0.2;

    // listOfTelephones --> String s telefonnim cislem + double[] array, kde v [0] je pocet hovoru a v [1] je suma za hovory
    private final Map<String, double[]> listOfTelephones;
    private String phoneWithHighestNumberOfCalls;

    public TelephoneBillCalculatorImpl(HashMap<String, double[]> map) {
        listOfTelephones = map;
    }

    // 4 varianty 1) zapocne a skonci mimo hlavni tarif ++ 2) zapocne a skonci v hlavnim tarifu ++ 3) zapocne mimo tarif a skonci v nem ++ 4) zapocne v tarifu a skonci mimo nej
    private double getBasicSumOfBill(LocalDateTime callBegin, LocalDateTime callEnd, long minutes) {
        if (((callBegin.getHour() == 7) && (callEnd.getHour() == 8)) || ((callBegin.getHour() == 15) && (callEnd.getHour() == 16))) { // specificke pripady, okrajove hodnoty
            double tax = callBegin.getHour() == 7 ? OUT_OF_BASIC_INTERVAL_CALL_TAX : BASIC_CALL_TAX;
            long minutesBegin = 60 - callBegin.getMinute();
            double secondTax = (tax == OUT_OF_BASIC_INTERVAL_CALL_TAX) ? BASIC_CALL_TAX : OUT_OF_BASIC_INTERVAL_CALL_TAX;
            long minutesEnd = minutes - minutesBegin;

            return (minutesBegin * tax) + (minutesEnd * secondTax);
        } else {
            double tax = ((callEnd.getHour() < 8) || (callBegin.getHour() > 16)) ? OUT_OF_BASIC_INTERVAL_CALL_TAX : BASIC_CALL_TAX;
            return minutes * tax;
        }
    }

    public BigDecimal calculate (String phoneLog) {
        double sum;

        String[] calls = phoneLog.split(",");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        LocalDateTime callBegin = LocalDateTime.parse(calls[1], formatter);
        LocalDateTime callEnd = LocalDateTime.parse(calls[2], formatter);

        long seconds = ChronoUnit.SECONDS.between(callBegin, callEnd);

        // resi bod 2, tedy hovory s delsim casovym rozpetim nez 5 minut a se slevou
        if (seconds > 300) {
            long cheaperSeconds = seconds - 300;
            LocalDateTime updatedCallEnd = callBegin.plus(Duration.of(301, ChronoUnit.SECONDS));

            int minutes = (int) (cheaperSeconds / 60.0);
            sum = getBasicSumOfBill(callBegin, updatedCallEnd, 5);
            sum += (cheaperSeconds % 60 > 0 ? minutes+1 : minutes) * MORE_THAN_FIVE_MINUTES_CALL_TAX;
        } else {
            int minutes = (int) (seconds / 60.0);
            minutes = (seconds % 60 > 0 ? minutes+1 : minutes);

            sum = getBasicSumOfBill(callBegin, callEnd, minutes);
        }

        // zde se prirazuji hodnoty k telefonnimu cislu --> {"420777666555" -> {1, 245.5}} --> {"PHONE_NUMBER" -> {NUMBER_OF_CALLS, SUM_OF_BILL}}
        // uchovavat si i v globalni promene i cislo telefonu a maximalni hodnotu jeho sumy? --> neradi se dle sumy, ale dle poctu volani. Prvni if tedy pouze inicializuje prvni volane cislo.
        // porovnavat tedy pouze pocet hovoru a v pripade, ze bude pocet hovoru stejny, tak zohlednit aritmeticky nejvyssi hodnotu
        if (this.listOfTelephones.get(calls[0]) == null) {
            double[] callInfo = {1.0, sum};
            this.listOfTelephones.put(calls[0], callInfo);
            this.phoneWithHighestNumberOfCalls = (this.phoneWithHighestNumberOfCalls == null) ? calls[0] : this.phoneWithHighestNumberOfCalls;
        } else {
            double[] callInfo = this.listOfTelephones.get(calls[0]);
            double[] highestNumOfCallsInfo = this.listOfTelephones.get(this.phoneWithHighestNumberOfCalls);
            callInfo[0] += 1;
            callInfo[1] += sum;
            this.listOfTelephones.put(calls[0], callInfo);

            if ((int) highestNumOfCallsInfo[0] < (int) callInfo[0]) {
                this.phoneWithHighestNumberOfCalls = calls[0];
            } else if ((int) highestNumOfCallsInfo[0] == (int) callInfo[0]) { // stejny pocet hovoru, aritmeticky nejvyssi hodnota (castka/pocet_hovoru)
                double oldCallArithmetic = highestNumOfCallsInfo[1] / highestNumOfCallsInfo[0];
                double newCallArithmetic = callInfo[1] / callInfo[0];
                this.phoneWithHighestNumberOfCalls = (Double.compare(oldCallArithmetic, newCallArithmetic) < 0) ? calls[0] : this.phoneWithHighestNumberOfCalls;
            }
        }

        // System.out.println(sum); // for testing

        return new BigDecimal(sum);
    }

    // V ramci aplikace se hovori o nejakem souboru, ktery ale neni definovan. Prozatim uzit defaultni soubor --> spousteno s argumentem "calculator.csv"
    public static void main(String[] args) {
        String line;
        HashMap<String, double[]> listOfTelephones = new HashMap<>();
        TelephoneBillCalculatorImpl telephoneBillCalculator = new TelephoneBillCalculatorImpl(listOfTelephones);

        try {
            BufferedReader br = new BufferedReader(new FileReader(args[0]));
            while ((line = br.readLine()) != null) {
                telephoneBillCalculator.calculate(line);
            }

            // zde provest vypis jednotlivych cisel s jejich sazbou hovoru + nastaveni sazby u nejcasteji volaneho cisla na nulu
            for (String key : listOfTelephones.keySet()) {
                if (key.equals(telephoneBillCalculator.phoneWithHighestNumberOfCalls)) {
                    listOfTelephones.computeIfPresent(key, (k, v) -> {
                        v[1] = 0.0;
                        return v;
                    });
                }
                System.out.println(key + " >> number of calls: " + (int) listOfTelephones.get(key)[0] + ", total sum: " + listOfTelephones.get(key)[1]);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
