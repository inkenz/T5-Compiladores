package br.ufscar.dc.compiladores.t5;

import static br.ufscar.dc.compiladores.t5.LinguagemLAUtils.verificarTipo;

import br.ufscar.dc.compiladores.t5.LAParser.CmdContext;
import br.ufscar.dc.compiladores.t5.LAParser.CmdFacaContext;
import br.ufscar.dc.compiladores.t5.LAParser.CmdParaContext;
import br.ufscar.dc.compiladores.t5.LAParser.ParametroContext;
import br.ufscar.dc.compiladores.t5.TabelaDeSimbolos.Tipo;

public class LAGeradorC extends LABaseVisitor<Void> {
    StringBuilder saida;
    Escopo escopos; 

    public LAGeradorC() {
        saida = new StringBuilder();
        this.escopos = new Escopo();
    }

    @Override
    public Void visitPrograma(LAParser.ProgramaContext ctx) {
        escopos.criarNovoEscopo();

        saida.append("#include <stdio.h>\n");
        saida.append("#include <stdlib.h>\n");
        saida.append("\n");
        
        for(var ctxDec : ctx.declaracoes().decl_local_global()){
            if(ctxDec.declaracao_global() != null){
                visitDeclaracao_global(ctxDec.declaracao_global());
            } else {
                visitDeclaracao_local(ctxDec.declaracao_local());
            }
        }

        saida.append("\n");
        saida.append("int main() {\n");

        ctx.corpo().declaracao_local().forEach(dec -> visitDeclaracao_local(dec));

        ctx.corpo().cmd().forEach(cmd -> visitCmd(cmd));

        saida.append("return 0;\n");
        saida.append("}\n");
        return null;
    }

    @Override
    public Void visitDeclaracao_global(LAParser.Declaracao_globalContext ctx) {
        TabelaDeSimbolos tabela = escopos.primeiroEscopo();
        if (ctx.PROCEDIMENTO() != null) {
            saida.append("void " + ctx.IDENT().getText() + "(");
            tabela.inserir(ctx.IDENT().getText(), Tipo.PROCEDIMENTO);
            escopos.criarNovoEscopo();
            escopos.escopoAtual().inserir(ctx.IDENT().getText(), Tipo.PROCEDIMENTO);
            if (ctx.parametros() != null) {
                visitParametros(ctx.parametros());
            }
            saida.append(") {\n");
            saida.append(ctx.IDENT().getText() + "\n");
            saida.append("}\n");
        } else if (ctx.FUNCAO() != null) {
            saida.append(TipoC(ctx.tipo_estendido()) + " " + ctx.IDENT().getText() + "(");
            tabela.inserir(ctx.IDENT().getText(), Tipo.FUNCAO);
            escopos.criarNovoEscopo();
            escopos.escopoAtual().inserir(ctx.IDENT().getText(), Tipo.FUNCAO);
            escopos.escopoAtual().inserir(ctx.IDENT().getText()+".return", verificarTipo(ctx.tipo_estendido()));
            if (ctx.parametros() != null) {
                visitParametros(ctx.parametros());
            }
            saida.append(") {\n");
            saida.append(ctx.IDENT().getText() + "\n");
            saida.append("}\n");
        }
        return null;
    }

    @Override
    public Void visitDeclaracao_local(LAParser.Declaracao_localContext ctx) {
        if (ctx.DECLARE() != null) {
            visitVariavel(ctx.variavel());
        } else if (ctx.CONSTANTE() != null) {
            String ident = ctx.IDENT().getText();
            String valorConstante = ctx.valor_constante().getText();
            saida.append("#define " + " " + ident + " " + valorConstante + ";\n");

        } else if (ctx.TIPO() != null) {
            Tipo tipo2 = verificarTipo(escopos, ctx.tipo()); 
            String ident = ctx.IDENT().getText();
            saida.append("typedef struct{\n");
            visitRegistro(ctx.tipo().registro());
            saida.append("}" + ctx.IDENT().getText() + ";\n");
            
            escopos.escopoAtual().inserir(ident, tipo2);
        }
        return null;
    }

    @Override
    public Void visitRegistro(LAParser.RegistroContext ctx){
        ctx.variavel().forEach(var -> {
            saida.append("\t");
            visitVariavel(var);
        });
        return null;
    }

    @Override
    public Void visitCmd(LAParser.CmdContext ctx) {
        if (ctx.cmdEnquanto() != null) {
            visitCmdEnquanto(ctx.cmdEnquanto());
        } else if (ctx.cmdAtribuicao() != null) {
            visitCmdAtribuicao(ctx.cmdAtribuicao());
        } else if (ctx.cmdEscreva() != null) {
            visitCmdEscreva(ctx.cmdEscreva());
        } else if (ctx.cmdLeia() != null) {
            visitCmdLeia(ctx.cmdLeia());
        } else if (ctx.cmdRetorne() != null) {
            visitCmdRetorne(ctx.cmdRetorne());
        } else if (ctx.cmdSe() != null) {
            visitCmdSe(ctx.cmdSe());
        } else if (ctx.cmdFaca() != null) {
            visitCmdFaca(ctx.cmdFaca());
        } else if (ctx.cmdPara() != null) {
            visitCmdPara(ctx.cmdPara());
        }else if (ctx.cmdChamada() != null) {
            visitCmdChamada(ctx.cmdChamada());
        }else if (ctx.cmdCaso() != null) {
            visitCmdCaso(ctx.cmdCaso());
        }
        return null;
    }

    @Override
    public Void visitExp_aritmetica(LAParser.Exp_aritmeticaContext ctx) {
        if (ctx.termo().size() > 1) {
            for (int i = 0; i < ctx.termo().size(); i++) {
                visitTermo(ctx.termo(i));
                if (i < ctx.op1().size()) {
                    saida.append(" " + ctx.op1(i).getText() + " ");
                }
            }
        } else {
            visitTermo(ctx.termo(0));
        }
        return null;
    }

    @Override
    public Void visitCmdLeia(LAParser.CmdLeiaContext ctx) {
        ctx.identificador().forEach(id -> {
            Tipo tipo = verificarTipo(escopos, id.getText());
            String scanfFormat = "%d";
            if (tipo == Tipo.REAL) {
                scanfFormat = "%f";
            } else if (tipo == Tipo.LITERAL) {
                scanfFormat = "%s";
            }
            saida.append("scanf(\"" + scanfFormat + "\", &" + id.getText() + ");\n");
        });
        return null;
    }

    @Override
    public Void visitCmdEscreva(LAParser.CmdEscrevaContext ctx) {
        ctx.expressao().forEach(exp -> {
            Tipo tipo = verificarTipo(escopos, exp.getText());
            String printfFormat = "%d";
            if (tipo == Tipo.REAL) {
                printfFormat = "%f";
            } else if (tipo == Tipo.LITERAL) {
                printfFormat = "%s";
            }
            saida.append("printf(\"" + printfFormat + "\", " + exp.getText() + ");\n");
        });
        return null;
    }

    @Override
    public Void visitCmdSe(LAParser.CmdSeContext ctx) {
        saida.append("if (");
        visitExpressao(ctx.expressao());
        saida.append(") {\n");
        visitCmd(ctx.cmd(0));
        saida.append("}");
        if (ctx.cmd().size() > 1) {
            saida.append(" else {\n");
            visitCmd(ctx.cmd(1));
            saida.append("}\n");
        }
        return null;
    }

    @Override
    public Void visitCmdEnquanto(LAParser.CmdEnquantoContext ctx) {
        saida.append("while (");
        visitExpressao(ctx.expressao());
        saida.append(") {\n");
        ctx.cmd().forEach(cmdCtx -> visitCmd(cmdCtx));
        saida.append("}\n");
        return null;
    }

    @Override
    public Void visitCmdAtribuicao(LAParser.CmdAtribuicaoContext ctx) {
        Tipo tipo = verificarTipo(escopos, ctx.identificador());
        if (tipo == Tipo.LITERAL){
            saida.append("strcpy(" + ctx.identificador().getText() + ", ");
            visitExpressao(ctx.expressao());
            saida.append(")");
        }
        else{
            if (ctx.PONTEIRO() != null){
                saida.append("*");
            }
            saida.append(ctx.identificador().getText() + " = ");
            visitExpressao(ctx.expressao());
        }
        saida.append(";\n");
        return null;
    }

    @Override
    public Void visitCmdRetorne(LAParser.CmdRetorneContext ctx) {
        saida.append("return ");
        visitExpressao(ctx.expressao());
        saida.append(";\n");
        return null;
    }
    
    @Override
    public Void visitCmdPara(CmdParaContext ctx) {
        saida.append("\n\tfor (" + ctx.IDENT().getText() + " = " + ctx.exp_aritmetica(0).getText() + "; " + ctx.IDENT().getText() + " <= " + ctx.exp_aritmetica(1).getText() + "; " + ctx.IDENT().getText() + "++) {\n");
        for (CmdContext cmdCtx: ctx.cmd()){
            saida.append("\t");
            visitCmd(cmdCtx);
        }
        saida.append("\t}\n");
        return null;
    }

    @Override
    public Void visitCmdFaca(CmdFacaContext ctx) {
        saida.append("\n\tdo {\n");
        for (CmdContext cmdCtx: ctx.cmd()){
            saida.append("\t");
            visitCmd(cmdCtx);
        }
        saida.append("\t} while (");
        visitExpressao(ctx.expressao());
        saida.append(");\n");
        return null;
    }

    @Override
    public Void visitCmdChamada(LAParser.CmdChamadaContext ctx) {
        saida.append(ctx.IDENT().getText() + "(");
        if (ctx.expressao() != null && ctx.expressao().size() > 0) {
            for (int i = 0; i < ctx.expressao().size(); i++) {
                visitExpressao(ctx.expressao(i));
                if (i < ctx.expressao().size() - 1) {
                    saida.append(", ");
                }
            }
        }
        saida.append(");\n");
        return null;
    }

    @Override
    public Void visitCmdCaso(LAParser.CmdCasoContext ctx) {
        String varSwitch = ctx.exp_aritmetica().getText();
        saida.append("switch (" + varSwitch + ") {\n");
        visitSelecao(ctx.selecao());
        if (ctx.cmd() != null && ctx.cmd().size() > 0) {
            for (int i = 0; i < ctx.cmd().size(); i++) {
                visitCmd(ctx.cmd(i));
            }
        }
        saida.append("}\n");
        return null;
    }

    @Override
    public Void visitExpressao(LAParser.ExpressaoContext ctx) {
        int numTermosLogicos = ctx.termo_logico().size();
        for (int i = 0; i < numTermosLogicos; i++) {
            visitTermo_logico(ctx.termo_logico(i));
            if (i < numTermosLogicos - 1) {
                saida.append(" || ");
            }
        }
        return null;
    }

    @Override
    public Void visitTermo_logico(LAParser.Termo_logicoContext ctx) {
        int numFatoresLogicos = ctx.fator_logico().size();
        for (int i = 0; i < numFatoresLogicos; i++) {
            visitFator_logico(ctx.fator_logico(i));
            if (i < numFatoresLogicos - 1) {
                saida.append(" && ");
            }
        }
        return null;
    }

    @Override
    public Void visitFator_logico(LAParser.Fator_logicoContext ctx) {
        if (ctx.NAO() != null) {
            saida.append("!");
        }
        visitParcela_logica(ctx.parcela_logica());
        return null;
    }

@Override
public Void visitParcela_logica(LAParser.Parcela_logicaContext ctx) {
    if (ctx.VERDADEIRO() != null) {
        saida.append("1");
    } else if (ctx.FALSO() != null) {
        saida.append("0");
    } else {
        visitExp_relacional(ctx.exp_relacional());
    }
    return null;
}


    @Override
    public Void visitTermo(LAParser.TermoContext ctx) {
        ctx.fator().forEach(fator -> {
            visitFator(fator);
            if (ctx.op2() != null) {
                saida.append(ctx.op2(0).getText());
            }
        });
        return null;
    }

    @Override
    public Void visitFator(LAParser.FatorContext ctx) {
        ctx.parcela().forEach(parcela -> visitParcela(parcela));
        return null;
    }

    @Override
    public Void visitParcela(LAParser.ParcelaContext ctx) {
        if (ctx.parcela_unario() != null) {
            visitParcela_unario(ctx.parcela_unario());
        } else {
            visitParcela_nao_unario(ctx.parcela_nao_unario());
        }
        return null;
    }

    @Override
    public Void visitParcela_unario(LAParser.Parcela_unarioContext ctx) {
        if (ctx.identificador() != null) {
            saida.append(ctx.identificador().getText());
        } else if (ctx.IDENT() != null) {
            saida.append(ctx.IDENT().getText());
        } else if (ctx.NUM_INT() != null) {
            saida.append(ctx.NUM_INT().getText());
        } else if (ctx.NUM_REAL() != null) {
            saida.append(ctx.NUM_REAL().getText());
        } else if (ctx.expressao() != null) {
            saida.append("(");
            for(int i = 0; i<ctx.expressao().size(); i++){
                if(i>0) saida.append(", ");
                visitExpressao(ctx.expressao().get(i));
            }
              
            saida.append(")");
        }
        return null;
    }

    @Override
    public Void visitParcela_nao_unario(LAParser.Parcela_nao_unarioContext ctx) {
        if (ctx.ENDERECO() != null) {
            saida.append("&");
            if (ctx.identificador() != null) {
                saida.append(ctx.identificador().getText());
            }
        } else if (ctx.CADEIA() != null) {
            saida.append(ctx.CADEIA().getText());
        }
        return null;
    }


    @Override
    public Void visitParametros(LAParser.ParametrosContext ctx) {
        int quantidadeParametros = ctx.parametro().size();
        for (int i = 0; i < quantidadeParametros; i++) {
            ParametroContext parametro = ctx.parametro().get(i);
            String tipo = TipoC(parametro.tipo_estendido());
            saida.append(tipo + " " + parametro.identificador(0).IDENT(0).getText());
            if (i < quantidadeParametros - 1) {
                saida.append(", ");
            }
        }
        return null;
    }

    private String TipoC(LAParser.TipoContext ctx) {
        if (ctx.registro() != null) {
            return "struct " + ctx.registro().getParent().getChild(1).getText();
        } else if (ctx.tipo_estendido() != null) {
            return TipoC(ctx.tipo_estendido());
        } else {
            return "void";
        }
    }
    
    

    private String TipoC(LAParser.Tipo_estendidoContext ctx) {
        if (ctx.tipo_basico() != null) {
            if (ctx.IDENT() != null) {
                return ctx.IDENT().getText();
            } else {
                return TipoC(ctx.tipo_basico());
            }
        } else {
            return "void";
        }
    }

    private String TipoC(LAParser.Tipo_basicoContext ctx) {
    if (ctx.LITERAL() != null) {
        return "char*"; 
    } else if (ctx.INTEIRO() != null) {
        return "int";
    } else if (ctx.REAL() != null) {
        return "float";
    } else if (ctx.LOGICO() != null) {
        return "int"; 
    } else {
        return "void"; 
    }
}

    @Override
    public Void visitVariavel(LAParser.VariavelContext ctx) {
        Tipo tipo = verificarTipo(escopos, ctx.tipo());

        if (tipo != Tipo.REGISTRO && ctx.tipo() == null ){
            String strCtipo = TipoC(ctx.tipo());

            saida.append("\t" + strCtipo);

            if (tipo == Tipo.PONTEIRO){
                saida.append("*");
            }

            saida.append(" " + ctx.identificador(0).getText());

            escopos.escopoAtual().inserir(ctx.identificador(0).IDENT(0).getText(), tipo);

            if (tipo == Tipo.LITERAL){
                saida.append("[50]");
            }
            
            for (int i = 0; i < ctx.VIRGULA().size(); i++){
                saida.append(", " + ctx.identificador(i + 1).getText());
                escopos.escopoAtual().inserir(ctx.identificador(i + 1).IDENT(0).getText(), tipo);
                if (tipo == Tipo.LITERAL){
                    saida.append("[50]");
                }
            }
        }
        else if (tipo == Tipo.REGISTRO){
            escopos.criarNovoEscopo();
            saida.append("\tstruct {\n");
            visitRegistro(ctx.tipo().registro());
            saida.append("\t} " + ctx.identificador(0).getText());
            
            escopos.escopoAtual().inserir(ctx.identificador(0).getText(), tipo);
        }
        else {
            String tipoC = TipoC(ctx.tipo());

            saida.append(tipoC);
            saida.append(" " + ctx.identificador(0).getText());
            escopos.escopoAtual().inserir(ctx.identificador(0).getText(), tipo);
            
            for (int i = 0; i < ctx.VIRGULA().size(); i++){
                saida.append(", " + ctx.identificador(i + 1).getText());
                escopos.escopoAtual().inserir(ctx.identificador(i + 1).getText(), tipo);
            }
        }
        saida.append(";\n");

        return null;
    }

    @Override
    public Void visitTipo_estendido(LAParser.Tipo_estendidoContext ctx) {
        TabelaDeSimbolos tabela = escopos.escopoAtual();
        if (ctx.IDENT() != null) {
            String tipo = ctx.IDENT().getSymbol().getText();
            Tipo tipoSimbolo = tabela.verificar(tipo);
            saida.append(tipoSimbolo);
        } else {
            saida.append("void");
        }
        return null;
    }

    @Override
    public Void visitSelecao(LAParser.SelecaoContext ctx) {
        ctx.item_selecao().forEach(itemSelecao -> visitItem_selecao(itemSelecao));
        return null;
    }

    @Override
    public Void visitItem_selecao(LAParser.Item_selecaoContext ctx) {
        String constantes = ctx.constantes().getText();
        StringBuilder cmd = new StringBuilder("{\n");
        for (LAParser.CmdContext cmdContext : ctx.cmd()) {
            cmd.append(cmdContext.getText());
        }
        cmd.append("}\n");

        if (ctx.constantes().numero_intervalo() != null) {
            saida.append("default: ");
            saida.append(cmd);
        } else {
            saida.append("case ").append(constantes).append(": ");
            saida.append(cmd);
        }
        return null;
    }

    @Override
    public Void visitOp_relacional(LAParser.Op_relacionalContext ctx) {
        if (ctx.IGUAL() != null) {
            saida.append(" == ");
        }
        else if (ctx.DIFERENTE() != null) {
            saida.append(" != ");
        }
        else if (ctx.MAIOR() != null) {
            saida.append(" > ");
        }
        else if (ctx.MENOR() != null) {
            saida.append(" < ");
        }
        else if (ctx.MAIORIGUAL() != null) {
            saida.append(" >= ");
        }
        else if (ctx.MENORIGUAL() != null) {
            saida.append(" <= ");
        }
        return null;
    }

}

