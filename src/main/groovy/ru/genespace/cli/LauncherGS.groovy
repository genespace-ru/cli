package ru.genespace.cli;

import com.beust.jcommander.JCommander
import com.beust.jcommander.MissingCommandException
import com.beust.jcommander.Parameters
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import java.lang.reflect.Method

import static nextflow.Const.*

import nextflow.exception.AbortOperationException
import nextflow.util.LoggerHelper
import nextflow.cli.CliOptions
import nextflow.cli.CmdBase
import nextflow.cli.CmdRun
import nextflow.cli.Launcher
import nextflow.cli.UsageAware

import static nextflow.util.SysHelper.dumpThreads

@Slf4j
@CompileStatic
public class LauncherGS extends Launcher {

    private List<CmdBase> allMyCommands
    private JCommander jcommander
    private CliOptions optionsMy
    private String cliStringMy
    private String colsString
    private List<String> normalizedArgs
    private CmdBase command
    private boolean runInternal = false
    private boolean fullVersion

    private static final String[] EMPTY = new String[0]

    LauncherGS(String[] args) {
        super();
        init();
    }

    @Override
    protected void init() {
        super.init()
        //Add other commands here
        allMyCommands = (List<CmdBase>) [
            new CmdConvert(),
            //new CmdHelpGS(),
            new CmdConvertRun()
        ]
        optionsMy = new CliOptions()
        jcommander = new JCommander(optionsMy)
        for (CmdBase cmd : allMyCommands) {
            cmd.launcher = this;
            jcommander.addCommand(cmd.name, cmd, aliases(cmd))
        }
        //???
        jcommander.setProgramName(APP_NAME)
        sleep(3000)
    }

    private static String[] aliases(CmdBase cmd) {
        final aliases = cmd.getClass().getAnnotation(Parameters)?.commandNames()
        return aliases ?: EMPTY
    }

    /**
     * Create the Jcommander 'interpreter' and parse the command line arguments
     */
    Launcher parseMainArgs(String... args) {
        this.cliStringMy = makeCli(System.getenv('NXF_CLI'), args)
        this.colsString = System.getenv('COLUMNS')

        def cols = getColumns()
        if (cols)
            jcommander.setColumnSize(cols)

        normalizedArgs = normalizeArgs(args)
        try {
            jcommander.parse(normalizedArgs as String[])
        }
        catch (MissingCommandException e) {
            return this;
        }
        fullVersion = '-version' in normalizedArgs
        command = allMyCommands.find { it.name == jcommander.getParsedCommand() }
        // set the log file name
        checkLogFileName()

        return this
    }

    CliOptions getOptions() {
        optionsMy
    }

    String getCliString() {
        cliStringMy
    }

    /**
     * normalize the command line arguments to handle some corner cases
     */
    List<String> normalizeArgs(String... args) {

        def argList = args.toList()
        if(args.size() > 0) {
            def current = args[0]
            boolean isGsCommand = allMyCommands.find{it.name == current} != null
            boolean isParentCommand = super.findCommand(current ) != null

            if (!isGsCommand && !isParentCommand && new File(current).isFile()) {
                argList.add(0, CmdConvertRun.NAME)
            }
        }
        String[] newArgs = argList as String[]
        return callSuperNormalizeArgs(newArgs)
    }

    List<String> callSuperNormalizeArgs(String... args) {
        Method method = nextflow.cli.Launcher.class.getDeclaredMethod("normalizeArgs", String[].class)
        method.setAccessible(true)
        // Cast to Object to prevent Groovy from spreading the array into multiple arguments
        return (List<String>) method.invoke(this, (Object) args)
    }

    static private boolean isValue(String x) {
        if (!x) return false                   // an empty string -> not a value
        if (x.size() == 1) return true         // a single char is not an option -> value true
        !x.startsWith('-') || x.isNumber() || x.contains(' ')
    }

    CmdBase findCommand(String cmdName) {
        CmdBase cmd = allMyCommands.find { it.name == cmdName }
        if (cmd)
            return cmd;
        return super.findCommand(cmdName)
    }

    /**
     * Print the usage string for the given command - or -
     * the main program usage string if not command is specified
     * @param command The command for which get help or {@code null}
     * @return The usage string
     */
    @Override
    void usage(String command = null) {

        if (command) {
            def exists = allMyCommands.find { it.name == command } != null
            if (!exists) {
                super.usage(command)
                return
            }

            jcommander.usage(command)
            return
        }

        println "Usage: genespace [options] COMMAND [arg...]\n "
        super.printOptions(CliOptions)
        printCommands(allMyCommands)
        //get super allCommands by reflection
        def field = nextflow.cli.Launcher.class.getDeclaredField("allCommands")
        field.setAccessible(true)
        def commands = (List<CmdBase>) field.get(this)
        printCommands(commands)
    }

    private void checkForHelpGS() {
        if (options.help || !command || command.help) {
            if (command instanceof UsageAware) {
                (command as UsageAware).usage()
                // reset command to null to skip default execution
                command = null
                return
            }

            // replace the current command with the `help` command
            def target = command?.name
            //use CmdHelpGS for  help
            command = allMyCommands.find { it instanceof CmdHelpGS }
            if (target) {
                (command as CmdHelpGS).args = [target]
            }
        }
    }

    /**
     * Launch the pipeline execution
     */
    @Override
    int run() {
        if (command) {
            /*
             * setup environment
             */
            //setupEnvironment()
            /*
             * Real execution starts here
             */
            try {
                log.debug '$ > ' + cliString

                // -- print out the version number, then exit
                if (options.version) {
                    println getVersion(fullVersion)
                    return 0
                }

                // -- print out the program help, then exit
                checkForHelpGS()

                // launch the command
                command?.run()

                if (log.isTraceEnabled())
                    log.trace "Exit\n " + dumpThreads()
                return 0
            }
            catch (AbortOperationException e) {
                def message = e.getMessage()
                if (message)
                    System.err.println(LoggerHelper.formatErrMessage(message, e))
                log.debug("Operation aborted ", e.cause ?: e)
                return (1)
            }
            //TODO: add real exceptions from command
            catch (IOException e) {
                log.error(e.message, e)
                return (1)
            }

            catch (Throwable fail) {
                log.error("@unknown ", fail)
                return (1)
            }
            return 0;
        }
        else
            return super.run();
    }



    Launcher command(String[] args) {
        //        // TODO parse command here and repla ce args if needed
        //        if(args.length == 0)
        //            return super.command( args );
        //        if(args[0].equals( "convert " )) {
        //        }
        /*
         * CLI argument parsing
         */
        try {
            parseMainArgs(args)
            if (!command) {
                return super.command(args)
            }

            LoggerHelper.configureLogger(this)
        }
        //???        catch( ParameterException e ) {
        //            // print command line parsing errors
        //            // note: use  system.err.println since if an exception is raised
        //            //       parsing the cli params the logging is not configured
        //            System.err.println  "${e.getMessage()} -- Check the available commands and options and syntax with 'help' "
        //            System.exit(1)
        //
        //        }
        catch (AbortOperationException e) {
            final msg = e.message ?: "Unknown abort reason "
            System.err.println(LoggerHelper.formatErrMessage(msg, e))
            System.exit(1)
        }
        catch (Throwable e) {
            e.printStackTrace(System.err)
            System.exit(1)
        }
        return this
        //return super.command( args );
    }

    private short getColumns() {
        if (!colsString) {
            return 0
        }
        try {
            colsString.toShort()
        }
        catch (Exception e) {
            log.debug "Unexpected terminal \$COLUMNS value: $colsString "
            return 0
        }
    }

    private void checkLogFileName() {
        if (!optionsMy.logFile) {
            //            if( isDaemon() )
            //                options.logFile = System .getenv('NXF_LOG_FILE') ?: '.node-nextflow.log'
            //            else
            if (command instanceof CmdConvert || optionsMy.debug || optionsMy.trace)
                optionsMy.logFile = System.getenv('NXF_LOG_FILE') ?: ".nextflow.log"
        }
    }

    static void main(String[] args) {
        Launcher launcher = new LauncherGS(args)
        Launcher launcher2 = launcher.command(args)
        final status = launcher2.run()
        if (status)
            System.exit(status)
    }
}