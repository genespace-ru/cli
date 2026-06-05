package ru.genespace.cli;

import nextflow.cli.Launcher;

public class LauncherGS extends Launcher {
    LauncherGS(String[] args) {
        super();
    }

    @Override
    public int run() {
        //System.out.println( "Running command groovy" );
        return super.run();
    }

    static void main(String[] args) {
        final status = new LauncherGS(args).command(args).run()
        if( status )
            System.exit(status)
    }
    @Override
    public Launcher command(String... args) {
        // TODO parse command here and replace args if needed
        return super.command( args );
    }
}
