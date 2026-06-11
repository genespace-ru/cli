package ru.genespace.cli;

import java.nio.file.Path
import java.nio.file.Paths
import biouml.model.Diagram
import biouml.model.util.DiagramXmlReader
import biouml.plugins.wdl.nextflow.NextFlowGenerator
import nextflow.cli.CmdRun
import nextflow.exception.AbortOperationException
import ru.biosoft.util.ApplicationUtils
import biouml.plugins.wdl.nextflow.NextFlowRunner

public class CmdConvertRun extends CmdRun {

    public static final NAME = 'rungs'
    @Override
    public void run() {
        //file to convert is first argument
        final inputFileStr =  args ? args[0] : null
        if( !inputFileStr )
            throw new AbortOperationException("No file to convert")
        Path inputPath = Paths.get(inputFileStr);
        // the target file is the second parameter
        // otherwise default the file in the same folder as input
        //final outputFileStr =  args && args.size()>1 ? args[1] : null
        String format = inputPath.getExtension()
        String inputformat = inputPath.getExtension() == "wdl" ? "wdl" : "diagram"
        Path outPath = outputDir ? Paths.get(outputDir) : Paths.get("." )
        Path nfPath = outPath.resolve(inputPath.getBaseName() + ".nf")
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
