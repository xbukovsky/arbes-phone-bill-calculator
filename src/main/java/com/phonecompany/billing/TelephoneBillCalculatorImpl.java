package com.phonecompany.billing;

import java.math.BigDecimal;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

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
        420774577453,13-01-2020 18:10:15,13-01-2020 18:12:57
        420776562353,18-01-2020 08:59:20,18-01-2020 09:10:00
        420776562353,19-01-2020 08:59:20,20-01-2020 09:10:00
        420776562353,27-01-2020 04:59:20,27-01-2020 05:10:00

        Výstupem metody je částka k uhrazení spočtena dle vstupního výpisu dle následujících pravidel:
    • Minutová sazba v intervalu <8:00:00,16:00:00) je zpoplatněna 1 Kč za každou započatou minutu.
    Mimo uvedený interval platí snížená sazba 0,50 Kč za každou započatou minutu.
    Pro každou minutu hovoru je pro stanovení sazby určující čas započetí dané minuty.

    • Pro hovory delší, než pět minut platí pro každou další započatou minutu nad rámec prvních pěti minut snížená sazba 0,20 Kč
    bez ohledu na dobu kdy telefonní hovor probíhá.

    • V rámci promo akce operátora dále platí, že hovory na nejčastěji volané číslo v rámci výpisu nebudou zpoplatněny.
    V případě, že by výpis obsahoval dvě nebo více takových čísel, zpoplatněny nebudou hovory na číslo s aritmeticky nejvyšší hodnotou.
     */
    public BigDecimal calculate (String phoneLog) {
        BigDecimal summary = new BigDecimal(0);

        String[] calls = phoneLog.split(",");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        LocalDateTime callBegin = LocalDateTime.parse(calls[1], formatter);
        LocalDateTime callEnd = LocalDateTime.parse(calls[2], formatter);

        long milliseconds = ChronoUnit.MILLIS.between(callBegin, callEnd);
        long minutes = ChronoUnit.MINUTES.between(callBegin, callEnd);
        long seconds = ChronoUnit.SECONDS.between(callBegin, callEnd);

        if (minutes >= 5 && (seconds - 300) > 0) {

            long cheaperMinutes = minutes - 5;
            if (seconds % 60 > 0) cheaperMinutes += 1;
            LocalDateTime endTime = callBegin.plus(Duration.of(5, ChronoUnit.MINUTES));

            // TODO resi bod 2, tedy hovory delsi nez 5 minut
            // TODO neresi cas volani
            // TODO prvnich 5 minut by se tedy resilo po starem, zbyle minuty by sly * 0.2


        } else {
            // TODO resi

            // TODO zde se resi 4 varianty -- 1) zapocne a skonci mimo hlavni tarif ++
            //  2) zapocne a skonci v hlavnim tarifu ++ 3) zapocne mimo tarif a skonci v nem ++ 4) zapocne v tarifu a skonci mimo nej

            summary.add(new BigDecimal(sum));

            // Nic jineho me nezajima, pak je tam delsi casove rozpeti, coz uz se pocita se slevou vyse
            if ((callEnd.getHour() < 8) || (callBegin.getHour() > 16)) { // levna sazba mimo hlavni hodiny za 0.5
                // todo lze zde rovnou pouzit return
                double sum = (callBegin.getSecond() < callEnd.getSecond() ? (minutes+1 * 0.5) : (minutes * 0.5));
            } else if((callBegin.getHour() >= 8) || (callEnd.getHour() < 16)) { // draha sazba za 1
                double sum = (callBegin.getSecond() < callEnd.getSecond() ? (minutes+1) : minutes);
            } else if((callBegin.getHour() == 7) && (callEnd.getHour() == 8)) { // specificke pripady, okrajove hodnoty
                long minutesBegin = 60 - callBegin.getMinute(); //7:57:25
                long minutesEnd = callEnd.getMinute(); // 8:03:42 --> 7 minut
                double sum = minutesBegin * 0.5;
                sum += (callBegin.getSecond() < callEnd.getSecond()) ? minutesEnd+1 : minutesEnd;
            } else if((callBegin.getHour() == 15) && (callEnd.getHour() == 16)) { // specificke pripady, okrajove hodnoty
                long minutesBegin = 60 - callBegin.getMinute(); //15:57:25
                long minutesEnd = callEnd.getMinute(); // 16:03:42 --> 7 minut
                double sum = minutesBegin;
                sum += (callBegin.getSecond() < callEnd.getSecond()) ? (minutesEnd+1 * 0.5) : (minutesEnd * 0.5);
            }

        }

        System.out.println(milliseconds);
        System.out.println(minutes);
        System.out.println(seconds);
        System.out.println("BEFORE GET HOUR AND SECOND");
        System.out.println(callBegin.getHour());
        System.out.println(callBegin.getSecond());

        return new BigDecimal(123456);
    }

    // TODO v ramci aplikace se hovori o nejakem souboru, prozatim uzit defaultni soubor --> spousteno s argumentem "calculator.csv"
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
