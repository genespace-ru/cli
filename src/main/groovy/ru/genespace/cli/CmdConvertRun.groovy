package ru.genespace.cli;

import java.nio.file.Path
import java.nio.file.Paths

import com.beust.jcommander.Parameters

import biouml.model.Diagram
import biouml.model.util.DiagramXmlReader
import biouml.plugins.wdl.nextflow.NextFlowGenerator
import nextflow.cli.CmdRun
import nextflow.exception.AbortOperationException
import ru.biosoft.util.ApplicationUtils
import biouml.plugins.wdl.nextflow.NextFlowRunner

@Parameters(commandDescription = "Convert a wdl script to a pipeline and execute")
public class CmdConvertRun extends CmdRun {

    public static final String NAME = 'rungs'
    @Override
    public void run() {
        //file to convert is first argument
        final inputFileStr =  args ? args[0] : null
        if( !inputFileStr )
            throw new AbortOperationException("No file to convert")
        Path inputPath = Paths.get(inputFileStr);
        String format = inputPath.getExtension()
        String inputformat = inputPath.getExtension() == "wdl" ? "wdl" : "diagram"
        Path outPath = outputDir ? Paths.get(outputDir) : Paths.get("." )
        Path nfPath = outPath.resolve(inputPath.getBaseName() + ".nf")
        //TODO: read input file to suggest format, do not depend on extension
        switch (format?.toLowerCase()) {
            case "nf":
                nfPath = inputPath
                break
            case "wdl":
                CmdConvert.convert(inputPath,nfPath, format)
                break
            default:
            //treat other file extensions as diagram file
                Diagram diagram = readDiagram(inputPath)
                CmdConvert.generateNextflow(diagram, nfPath)
        }

        args[0] = nfPath.toString();
        super.run();
    }

    protected static Diagram readDiagram(Path inputPath) {
        def name = inputPath.getBaseName()
        inputPath.toFile().withInputStream { fis ->
            return DiagramXmlReader.readDiagram(name, fis, null, null, null);
        }
    }

    @Override
    String getName() {
        NAME
    }
}
