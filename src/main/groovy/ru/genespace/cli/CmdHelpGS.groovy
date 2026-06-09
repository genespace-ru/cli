package ru.genespace.cli;

import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters

import groovy.transform.CompileStatic
import nextflow.cli.CmdHelp
import nextflow.cli.UsageAware

@CompileStatic
@Parameters(commandDescription = "Print the usage help for a command")
public class CmdHelpGS extends CmdHelp {

    @Parameter(names= '-helpgs', description = 'command name', arity = 1)

    private UsageAware getUsage( List<String> args ) {
        def result = args ? launcher.findCommand(args[0]) : null
        result instanceof UsageAware ? result as UsageAware: null
    }

    @Override
    void run() {
        def cmd = getUsage(args)
        if( cmd ) {
            cmd.usage(args.size()>1 ? args[1..-1] : Collections.<String>emptyList())
        }
        else {
            launcher.usage(args ? args[0] : null)
        }
    }
}
