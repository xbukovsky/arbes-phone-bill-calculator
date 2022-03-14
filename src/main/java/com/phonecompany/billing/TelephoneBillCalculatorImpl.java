package com.phonecompany.billing;

import java.math.BigDecimal;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
        String[] calls = phoneLog.split(",");
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

        try {
            Date callBegin = dateFormat.parse(calls[1]);
            Date callEnd = dateFormat.parse(calls[2]);
            long diff = callEnd.getTime() - callBegin.getTime();

            long diffSeconds = diff / 1000 % 60;
            long diffMinutes = diff / (60 * 1000) % 60;
            long diffHours = diff / (60 * 60 * 1000) % 24;
            long diffDays = diff / (24 * 60 * 60 * 1000);

            System.out.println(diff);
            System.out.print(diffDays + " days, ");
            System.out.print(diffHours + " hours, ");
            System.out.print(diffMinutes + " minutes, ");
            System.out.print(diffSeconds + " seconds.");
            System.out.println("\n \n");

        } catch(ParseException e) {
            e.printStackTrace();
        }

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
