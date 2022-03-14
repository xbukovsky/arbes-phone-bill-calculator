package com.phonecompany.billing;

import java.math.BigDecimal;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class TelephoneBillCalculatorImpl implements TelephoneBillCalculator {
    public static final double basicCallTax = 1;
    public static final double callTaxOutOfInterval = 0.5;
    public static final double callTaxMoreThanFiveMinutes = 0.2;

    // Vstup metody bude splnovat nize uvedena kriteria
    /*
        Vstupem metody je textový řetězec obsahující výpis volání. Výpis volání je ve formátu CSV s následujícími poli:
        • Telefonní číslo v normalizovaném tvaru obsahující pouze čísla (např. 420774567453)
        • Začátek hovoru ve tvaru dd-MM-yyyy HH:mm:ss
        • Konec hovoru ve tvaru dd-MM-yyyy HH:mm:ss

        Příklad obsahu souboru:
        420774577453,13-01-2020 18:10:15,13-01-2020 18:12:57 --> 3 minuty po 0.5 --> 1.5
        420776562353,18-01-2020 08:59:20,18-01-2020 09:01:00 --> 2 minuta po 1 --> 2
        420776562353,19-01-2020 08:59:20,20-01-2020 09:10:00 --> SILENA SUMA, jsou to rozdilne dny!
        420776562353,19-01-2020 08:59:20,19-01-2020 09:10:00 --> 5 minut po 1 + 6 minut po 0.2 --> 6.2
        420776562353,27-01-2020 04:59:20,27-01-2020 05:10:00 --> 5 minut po 0.5 + 6 minut po 0.2 --> 2.5 + 1.2 = 3.7
        420776562353,27-01-2020 07:57:25,27-01-2020 08:03:42 --> 3 minuty po 0.5 + 2 minuty po 1 + 2 minuty po 0.2 --> 3.9
        420776562353,27-01-2020 15:57:25,27-01-2020 16:01:42 --> 3 minuty po 1 + 2 minuty po 0.5 --> 4.0

        Výstupem metody je částka k uhrazení spočtena dle vstupního výpisu dle následujících pravidel:
    • Minutová sazba v intervalu <8:00:00,16:00:00) je zpoplatněna 1 Kč za každou započatou minutu.
    Mimo uvedený interval platí snížená sazba 0,50 Kč za každou započatou minutu.
    Pro každou minutu hovoru je pro stanovení sazby určující čas započetí dané minuty.

    • Pro hovory delší, než pět minut platí pro každou další započatou minutu nad rámec prvních pěti minut snížená sazba 0,20 Kč
    bez ohledu na dobu kdy telefonní hovor probíhá.

    • V rámci promo akce operátora dále platí, že hovory na nejčastěji volané číslo v rámci výpisu nebudou zpoplatněny.
    V případě, že by výpis obsahoval dvě nebo více takových čísel, zpoplatněny nebudou hovory na číslo s aritmeticky nejvyšší hodnotou.
     */

    // TODO tohle jeste upravit, jednou pouzivat minutes a podruhe sahat na minutesEnd () --> nelze to udelat jednoduseji? --> prvne specificke pripady
    private double getBasicSumOfBill(LocalDateTime callBegin, LocalDateTime callEnd, long minutes) {
        if (((callBegin.getHour() == 7) && (callEnd.getHour() == 8)) || ((callBegin.getHour() == 15) && (callEnd.getHour() == 16))) { // specificke pripady, okrajove hodnoty
            double tax = callBegin.getHour() == 7 ? 0.5 : 1;
            long minutesBegin = 60 - callBegin.getMinute(); //BEGIN 7:57:25 NEBO 15:57:25 --- END --- 8:03:42 --> 7 minut NEBO 16:03:42 --> 7 minut
            double secondTax = tax == 0.5 ? 1 : 0.5;
            long minutesEnd = minutes - minutesBegin;

            System.out.println("FIRST IF " + minutes);

            return (minutesBegin * tax) + (minutesEnd * secondTax);

        } else {
            double tax = ((callEnd.getHour() < 8) || (callBegin.getHour() > 16)) ? 0.5 : 1;
            System.out.println("SECOND IF with tax " + minutes * tax);
            return minutes * tax;
        }
    }

    public BigDecimal calculate (String phoneLog) {
        double sum = 0.0;

        String[] calls = phoneLog.split(",");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        LocalDateTime callBegin = LocalDateTime.parse(calls[1], formatter);
        LocalDateTime callEnd = LocalDateTime.parse(calls[2], formatter);

        long minutes = ChronoUnit.MINUTES.between(callBegin, callEnd);
        long seconds = ChronoUnit.SECONDS.between(callBegin, callEnd);

        if (seconds > 300) { // resi bod 2, tedy hovory s delsim casovym rozpetim nez 5 minut a se slevou
            System.out.println("first");

            long cheaperSeconds = seconds - 300;
            LocalDateTime updatedCallEnd = callBegin.plus(Duration.of(301, ChronoUnit.SECONDS));

            int mins = (int) (cheaperSeconds / 60.0);
            sum = getBasicSumOfBill(callBegin, updatedCallEnd, 5);
            sum += (cheaperSeconds % 60 > 0 ? mins+1 : mins) * 0.2;
        } else { // 4 varianty 1) zapocne a skonci mimo hlavni tarif ++ 2) zapocne a skonci v hlavnim tarifu ++ 3) zapocne mimo tarif a skonci v nem ++ 4) zapocne v tarifu a skonci mimo nej
            System.out.println("second");

            int mins = (int) (seconds / 60.0);
            mins = (seconds % 60 > 0 ? mins+1 : mins);

            sum = getBasicSumOfBill(callBegin, callEnd, mins);
        }

        System.out.println(sum);

        return new BigDecimal(sum);
    }

    // V ramci aplikace se hovori o nejakem souboru, ktery ale neni definovan. Prozatim uzit defaultni soubor --> spousteno s argumentem "calculator.csv"
    public static void main(String[] args) {
        String line = "";
        TelephoneBillCalculatorImpl telephoneBillCalculator = new TelephoneBillCalculatorImpl();

        try {
            BufferedReader br = new BufferedReader(new FileReader(args[0]));
            while ((line = br.readLine()) != null) {
                // TODO ma to vystup metody
                telephoneBillCalculator.calculate(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
