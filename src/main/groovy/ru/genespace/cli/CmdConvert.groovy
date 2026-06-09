package ru.genespace.cli;

import java.nio.file.Path
import java.nio.file.Paths
import biouml.model.Diagram
import biouml.model.util.DiagramXmlWriter
import biouml.plugins.wdl.diagram.WDLImporter
import biouml.plugins.wdl.diagram.WDLLayouter
import biouml.plugins.wdl.nextflow.NextFlowGenerator
import biouml.plugins.wdl.parser.AstStart
import biouml.plugins.wdl.parser.WDLParser
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters

import nextflow.exception.AbortOperationException
import nextflow.cli.CmdBase

import ru.biosoft.util.ApplicationUtils;

@Parameters(commandDescription = "Convert something to something")
public class CmdConvert extends CmdBase {
    static final public NAME = 'convert'
    private OutputStream stdout = System.out

    static final List<String> FORMATS = ['nextflow', 'diagram']

    @Parameter(names = ['-f','-format'], description = 'Output format')
    String format

    @Parameter(description = 'input file path, output file path')
    List<String> args

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
        Diagram diagram = loadDiagram(inputFileStr);
        if(format == "nextflow") {
            Path outputPath = outputFileStr ? Paths.get(outputFileStr) : inputPath.resolveSibling(inputPath.getBaseName() + ".nf" )
            NextFlowGenerator gen = new NextFlowGenerator();
            String nextFlow = gen.generate(diagram);
            ApplicationUtils.writeString(outputPath.toFile(), nextFlow);
        }
        else if(format == "diagram") {
            Path outputPath = outputFileStr ? Paths.get(outputFileStr) : inputPath.resolveSibling(inputPath.getBaseName() + ".dml")
            outputPath.toFile().withOutputStream{ fos ->
                DiagramXmlWriter writer = diagram.getType().getDiagramWriter();
                writer.setStream( fos );
                writer.write( diagram );
            }
        }
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
