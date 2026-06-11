package ru.genespace.cli;

import java.nio.file.Path
import java.nio.file.Paths

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters

import biouml.model.Diagram
import biouml.model.util.DiagramXmlWriter
import biouml.plugins.wdl.diagram.WDLImporter
import biouml.plugins.wdl.diagram.WDLLayouter
import biouml.plugins.wdl.nextflow.NextFlowGenerator
import biouml.plugins.wdl.nextflow.NextFlowRunner
import biouml.plugins.wdl.parser.AstStart
import biouml.plugins.wdl.parser.WDLParser
import nextflow.cli.CmdBase
import nextflow.exception.AbortOperationException
import ru.biosoft.util.ApplicationUtils

@Parameters(commandDescription = "Convert something to something")
public class CmdConvert extends CmdBase {
    static final public NAME = 'convert'
    private OutputStream stdout = System.out

    protected static final List<String> FORMATS = ['nextflow', 'diagram']

    @Parameter(names = ['-f','-format'], description = 'Output format')
    protected String format

    @Parameter(description = 'input file path, output file path')
    protected List<String> args

    @Override
    public void run() {
        if( !format )
            throw new AbortOperationException("No format name was specified")

        // -- validate command line options
        if( !FORMATS.contains(format ) ) {
            throw new AbortOperationException("Not a valid conversion format -- Specify one of " + FORMATS.join(", " ))
        }
        //file to convert is first argument
        final inputFileStr =  args ? args[0] : null
        if( !inputFileStr )
            throw new AbortOperationException("No file to convert")
        Path inputPath = Paths.get(inputFileStr);
        // the target file is the second parameter
        // otherwise default the file in the same folder as input
        final outputFileStr =  args && args.size()>1 ? args[1] : null

        println "Converting file " + inputPath.getName() + " to $format"

        String parent = inputPath.getParent().toString()

        String name = inputPath.getName();
        Path outputPath = null
        if(format == "nextflow")
            outputPath = outputFileStr ? Paths.get(outputFileStr) : inputPath.resolveSibling(inputPath.getBaseName() + ".nf" )
        else if(format == "diagram")
            outputPath = outputFileStr ? Paths.get(outputFileStr) : inputPath.resolveSibling(inputPath.getBaseName() + ".dml")

        convert(inputPath, outputPath, format);
    }

    protected static void convert (Path inputPath, Path outputPath, String format) {
        Diagram diagram = loadDiagram(inputPath.toString());
        if(format == "nextflow") {
            generateNextflow(diagram, outputPath)
        }
        else if(format == "diagram") {
            outputPath.toFile().withOutputStream{ fos ->
                DiagramXmlWriter writer = diagram.getType().getDiagramWriter();
                writer.setStream( fos );
                writer.write( diagram );
            }
        }
    }

    protected static void generateNextflow(Diagram diagram, Path outputPath) {
        NextFlowGenerator gen = new NextFlowGenerator();
        String nextFlow = gen.generate(diagram);
        outputPath.toFile().withOutputStream{ fos ->
            ApplicationUtils.writeString(fos, nextFlow)
        }
        NextFlowRunner.generateFunctions(outputPath.toAbsolutePath().parent.toString())
    }

    protected static Diagram loadDiagram(String path) throws Exception {
        File f = new File(path);

        String name = f.getName();
        name = f.getName().endsWith(".wdl") ? name.substring(0, name.length() - 4) : name;

        WDLImporter importer = new WDLImporter();

        String text = ApplicationUtils.readAsString(f);

        AstStart start = new WDLParser().parse(new StringReader(text));
        Diagram diagram = importer.generateDiagram(start, null, "diagram");
        new WDLLayouter().layout( diagram );
        return diagram;
    }

    @Override
    String getName() {
        NAME
    }
}
