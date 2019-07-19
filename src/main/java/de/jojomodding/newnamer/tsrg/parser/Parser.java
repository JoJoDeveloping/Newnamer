package de.jojomodding.newnamer.tsrg.parser;

import de.jojomodding.newnamer.tsrg.Tsrg;
import de.jojomodding.newnamer.tsrg.TsrgClass;
import de.jojomodding.newnamer.type.*;

import java.io.StringReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

public class Parser {

    TsrgClass current = null;
    Tsrg result = new Tsrg();
    int line = 0;

    public Tsrg parse(Stream<String> lines){
        lines.forEach(line -> {
            if(line.length() == 0) throw new ParsingError("Line of 0 length!", this.line);
            if(line.charAt(0)=='\t'){
                if(current == null) throw new ParsingError("Class members without preceding class definition!", this.line);
                parseFieldOrMethod(current, line.substring(1));
            }else{
                current = parseClassDef(line, result);
            }
            this.line++;
        });
        return result;
    }

    private TsrgClass parseClassDef(String line, Tsrg tsrg){
        String[] s = line.split(" ");
        if(s.length != 2) throw new ParsingError("Class definition with more than 1 space!", this.line);
        return tsrg.addClass(s[0], s[1]);
    }

    private void parseFieldOrMethod(TsrgClass clazz, String line){
        String[] s = line.split(" ");
        if(s.length == 2) parseField(clazz, s[0], s[1]);
        else if(s.length == 3) parseMethod(clazz, s[0], s[1], s[2]);
        else throw new ParsingError("Neither field nor method decl: "+line, this.line);
    }

    private void parseField(TsrgClass clazz, String notch, String srg){
        clazz.addField(notch, srg);
    }

    private void parseMethod(TsrgClass clazz, String notch, String type, String srg){
        FunctionType ft = parseFunctionType(new ParserHelper(type));
        clazz.addMethod(ft, notch, srg);
    }



    public static FunctionType parseFunctionTypeStatic(String type){
        return new Parser().parseFunctionType(type);
    }
    private FunctionType parseFunctionType(String s){
        return parseFunctionType(new ParserHelper(s));
    }
    private FunctionType parseFunctionType(ParserHelper chars) {
        if(chars.peek() != '(') throw new ParsingError("Expected function type, got "+chars.peek(), this.line);
        chars.pop();
        List<Type> args = new LinkedList<>();
        while(chars.peek() != ')')args.add(parseType(chars));
        chars.pop();
        Type result = parseType(chars);
        if(!chars.atEnd())
            throw new ParsingError("Unexpected end of type def!", this.line);
        return new FunctionType(result, args);
    }


    private Type parseType(ParserHelper chars) {
        switch (chars.peek()){
            case 'B':
            case 'C':
            case 'D':
            case 'F':
            case 'I':
            case 'J':
            case 'S':
            case 'V':
            case 'Z':
                return PrimitiveType.fromChar(chars.peekAndPop());
            case '[':
                chars.pop();
                return new ArrayType(parseType(chars));
            case 'L':
                chars.pop();
                return parseClassName(chars);
        }
        throw new ParsingError("Unknown char "+chars.peek(), line);
    }

    private Type parseClassName(ParserHelper chars) {
        StringBuilder sb = new StringBuilder();
        char c = chars.peekAndPop();
        if(!Character.isJavaIdentifierStart(c)) throw new ParsingError("Type name does not start with identifier start char!", this.line);
        sb.append(c);
        while (true){
            c = chars.peekAndPop();
            if(c != '/' && !Character.isJavaIdentifierPart(c))
                break;
            sb.append(c);
        }
        return new ClassType(sb.toString());
    }

    private class ParserHelper{
        private String s;
        private int i;

        public ParserHelper(String data) {
            this.s = data;
            this.i = 0;
        }

        public char peek(){
            if(i < s.length())
                return s.charAt(i);
            else throw new ParsingError("End of stream!", line);
        }

        public void pop(){
            i++;
        }

        public char popAndPeek(){
            i++;
            return peek();
        }

        public char peekAndPop(){
            char c = peek();
            i++;
            return c;
        }

        public boolean atEnd() {
            return i >= s.length();
        }
    }

}
