package de.jojomodding.newnamer.tsrg.parser;

public class ParsingError extends RuntimeException {

    private int line;

    public ParsingError(String cause, int line){
        super(cause);
        this.line = line;
    }

    @Override
    public String getLocalizedMessage() {
        return "Error at line "+line+": "+super.getMessage();
    }
}
