package br.ufscar.dc.compiladores.t5;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;

import br.ufscar.dc.compiladores.t5.LAParser.ProgramaContext;

public class LinguagemLA 
{
    public static void main( String[] args )
    {
        try (PrintWriter pw = new PrintWriter(new FileWriter(args[1]))){
            
            //Recebendo argumentos com arquivos de entrada e saída
            CharStream cs = CharStreams.fromFileName(args[0]);

            LALexer lex = new LALexer(cs);
            Boolean lexError = false;
            Token t = null;

            //Montagem do vocabulário
            while ((t = lex.nextToken()).getType() != Token.EOF) {
                String nomeToken = LALexer.VOCABULARY.getDisplayName(t.getType());

                if(nomeToken.equals("ERRO")) {
                    pw.println("Linha "+t.getLine()+": "+t.getText()+" - simbolo nao identificado");
                    lexError = true;
                    break;
                } else if(nomeToken.equals("CADEIA_NAO_FECHADA")) {
                    pw.println("Linha "+t.getLine()+": cadeia literal nao fechada");
                    lexError = true;
                    break;
                } else if(nomeToken.equals("COMENTARIO_NAO_FECHADO")) {
                    pw.println("Linha "+t.getLine()+": comentario nao fechado");
                    lexError = true;
                    break;
                } /* else {
                    pw.println("<'" + t.getText() + "'," + nomeToken + ">");
                } */
            }
            
            //Análise Sintática após verificação Léxica
            if(!lexError){
                cs = CharStreams.fromFileName(args[0]);

                lex = new LALexer(cs);
                CommonTokenStream tokens = new CommonTokenStream(lex);
                LAParser parser = new LAParser(tokens);

                parser.removeErrorListeners();
                MyCustomErrorListener mcel = new MyCustomErrorListener(pw);
                parser.addErrorListener(mcel);

                parser.programa();
            }
            
            cs = CharStreams.fromFileName(args[0]);
            lex = new LALexer(cs);
            CommonTokenStream tokens = new CommonTokenStream(lex);
            LAParser parser = new LAParser(tokens);

            parser.removeErrorListeners();

            ProgramaContext arvore = parser.programa();
            LinguagemLAVisitor as = new LinguagemLAVisitor();

            as.visitPrograma(arvore);
            LinguagemLAUtils.errosSemanticos.forEach((s) -> pw.println(s));
            
            if(LinguagemLAUtils.errosSemanticos.isEmpty()) {
                LAGeradorC agc = new LAGeradorC();
                agc.visitPrograma(arvore);
                pw.print(agc.saida.toString());
            }

            pw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
