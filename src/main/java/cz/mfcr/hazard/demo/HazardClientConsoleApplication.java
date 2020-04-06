package cz.mfcr.hazard.demo;

import com.google.common.base.Enums;
import cz.mfcr.hazard.demo.config.ClientProperties;
import cz.mfcr.hazard.demo.service.AisgService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@SpringBootApplication
@EnableConfigurationProperties(ClientProperties.class)
public class HazardClientConsoleApplication implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory
            .getLogger(HazardClientConsoleApplication.class);

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication();
        springApplication.setWebApplicationType(WebApplicationType.NONE);

        Set<String> sources = new HashSet<>();
        sources.add(HazardClientConsoleApplication.class.getName());
        springApplication.setSources(sources);
        springApplication.run(args);
    }

    @Autowired
    private AisgService service;

    private void printHelp() {
        System.out.println("Parametry:");
        System.out.println("\t--operace=Test|OveritOsobu|ZmenitUdajeOsoby|OveritOsobyHromadne|ZiskatVysledkyOveritOsobyHromadne");
        System.out.println("\t--input=cesta k souboru - JSON vstupni parametry operace - default: $operace.json");
        System.out.println("\t--outputXMLRequest=cesta k souboru pro uložení odchozího XML - default - System.out");
        System.out.println("\t--outputXMLResponse=cesta k souboru pro uložení vráceného XML - default - System.out");
        System.out.println("\t--outputJSONRequest=cesta k souboru pro uložení dat volání - default - System.out");
        System.out.println("\t--outputJSONResponse=cesta k souboru pro uložení vrácených dat - default - System.out");
        System.out.println("\n\n");
    }

    private OperaceEnum prepare(ApplicationArguments args) {
        LOG.info("Application started with command-line arguments: {}", Arrays.toString(args.getSourceArgs()));
        LOG.info("NonOptionArgs: {}", args.getNonOptionArgs());
        LOG.info("OptionNames: {}", args.getOptionNames());

        if (args.getSourceArgs().length == 0 || args.getNonOptionArgs().contains("?") || args.getOptionNames().contains("help")) {
            printHelp();
            return null;
        } else if (!args.getOptionNames().contains("operace")) {
            System.err.println("ERROR: není zadaná operace");
            printHelp();
            return null;
        }
        final String enumName = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(getArgument(args, "operace").orElse("UNKNOWN")), "_").toUpperCase();
        return Enums.getIfPresent(OperaceEnum.class, enumName).or(OperaceEnum.UNKNOWN);
    }

    private Optional<String> getArgument(final ApplicationArguments args, final String name) {
        if (args.containsOption(name)) {
            return args.getOptionValues(name).stream().findFirst();
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {

        OperaceEnum operace = prepare(args);

        if (operace == null) {
            return;
        }

        final String input = getArgument(args, "input").orElse(null);

        switch (operace) {
            case TEST:
                service.test();
                break;
            case OVERIT_OSOBU:
                service.overitOsobu(input);
                break;
            case ZMENIT_UDAJE_OSOBY:
                service.zmenitUdajeOsoby(input);
                break;
            case OVERIT_OSOBY_HROMADNE:
                service.overitOsobyHromadne(input);
                break;
            case ZISKAT_VYSLEDKY_OVERIT_OSOBY_HROMADNE:
                service.ziskatVysledkyOveritOsobyHromadne(input);
                break;
            default:
                System.out.println("Neznámá operace");
        }
    }
}
