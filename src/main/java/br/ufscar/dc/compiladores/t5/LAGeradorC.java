package br.ufscar.dc.compiladores.t5;

import static br.ufscar.dc.compiladores.t5.LinguagemLAUtils.verificarTipo;

import br.ufscar.dc.compiladores.t5.LAParser.Decl_local_globalContext;
import br.ufscar.dc.compiladores.t5.LAParser.Declaracao_globalContext;
import br.ufscar.dc.compiladores.t5.LAParser.Declaracao_localContext;
import br.ufscar.dc.compiladores.t5.LAParser.ParametroContext;
import br.ufscar.dc.compiladores.t5.LAParser.VariavelContext;
import br.ufscar.dc.compiladores.t5.TabelaDeSimbolos.Tipo;

import java.util.List;

import org.antlr.v4.runtime.tree.TerminalNode;

public class LAGeradorC extends LABaseVisitor<Void> {
    StringBuilder saida;
    TabelaDeSimbolos tabela;
    Escopo escopos = new Escopo();

    public LAGeradorC() {
        saida = new StringBuilder();
        this.tabela = new TabelaDeSimbolos();
    }

    @Override
    public Void visitPrograma(LAParser.ProgramaContext ctx) {
        saida.append("#include <stdio.h>\n");
        saida.append("#include <stdlib.h>\n");
        saida.append("\n");

        ctx.declaracoes().decl_local_global().forEach(dec -> visitDecl_local_global(dec));

        saida.append("\n");
        saida.append("int main() {\n");
        ctx.corpo().cmd().forEach(cmd -> visitCmd(cmd));
        saida.append("}\n");
        return null;
    }

    @Override
    public Void visitDecl_local_global(LAParser.Decl_local_globalContext ctx) {
        if (ctx.declaracao_local() != null) {
            visitDeclaracao_local(ctx.declaracao_local());
        } else if (ctx.declaracao_global() != null) {
            visitDeclaracao_global(ctx.declaracao_global());
        }
        return null;
    }

    @Override
    public Void visitDeclaracao_global(LAParser.Declaracao_globalContext ctx) {
        if (ctx.PROCEDIMENTO() != null) {
            saida.append("void " + ctx.IDENT().getText() + "(");
            if (ctx.parametros() != null) {
                visitParametros(ctx.parametros());
            }
            saida.append(") {\n");
            saida.append(ctx.IDENT().getText() + "\n");
            saida.append("}\n");
        } else if (ctx.FUNCAO() != null) {
            saida.append(TipoC(ctx.tipo_estendido()) + " " + ctx.IDENT().getText() + "(");
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
            String tipo = TipoC(ctx.tipo());
            LAParser.VariavelContext variavelCtx = ctx.variavel();
            for (LAParser.IdentificadorContext id : variavelCtx.identificador()) {
                saida.append(tipo + " " + id.getText());
                if (!variavelCtx.identificador(0).dimensao().isEmpty()) {
                    for (LAParser.DimensaoContext dim : variavelCtx.identificador().dimensao()) {
                        saida.append("[");
                        visitExp_aritmetica(dim.exp_aritmetica());
                        saida.append("]");
                    }
                }
                saida.append(";\n");
            }
        } else if (ctx.CONSTANTE() != null) {
            String ident = ctx.IDENT().getText();
            String tipoBasico = TipoC(ctx.tipo_basico());
            String valorConstante = ctx.valor_constante().getText();
            saida.append("const " + tipoBasico + " " + ident + " = " + valorConstante + ";\n");
        } else if (ctx.TIPO() != null) {
            String tipo = TipoC(ctx.tipo());
            String ident = ctx.IDENT().getText();
            saida.append("typedef " + tipo + " " + ident + ";\n");
        }
        return null;
    }

    @Override
    public Void visitCmd(LAParser.CmdContext ctx) {
        super.visitCmd(ctx);
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
        saida.append(ctx.identificador().getText() + " = ");
        visitExpressao(ctx.expressao());
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
    public Void visitExpressao(LAParser.ExpressaoContext ctx) {
        ctx.termo_logico().forEach(termo -> {
            visitTermo_logico(termo);
            saida.append(" || ");
        });
        return null;
    }

    @Override
    public Void visitTermo_logico(LAParser.Termo_logicoContext ctx) {
        ctx.fator_logico().forEach(fator -> {
            visitFator_logico(fator);
            saida.append(" && ");
        });
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
        if (ctx.identificador() != null) {
            String nomeIdentificador = ctx.identificador().getText();
            List<ParametroContext> parametros = tabela.verificar(nomeIdentificador).parametros;
            if (parametros != null) {
                int qtdParametros = parametros.size();
                if (qtdParametros > 0) {
                    saida.append(nomeIdentificador + "(");
                    for (int i = 0; i < qtdParametros; i++) {
                        visitExpressao(ctx.expressao().get(i));
                        if (i < qtdParametros - 1) {
                            saida.append(", ");
                        }
                    }
                    saida.append(")");
                } else {
                    saida.append(nomeIdentificador + "()");
                }
            } else {
                saida.append(nomeIdentificador + "()");
            }
        } else {
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
            return "struct " + ctx.getParent().IDENT().getText();
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
                return ctx.tipo_basico().getText();
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
        String tipo = TipoC(ctx.tipo());
        for(LAParser.IdentificadorContext id: ctx.identificador()){
            saida.append(tipo + " " + id.IDENT().get(0).getText());
            if(id.dimensao() != null) {
                for(LAParser.Exp_aritmeticaContext exp: id.dimensao().exp_aritmetica()){
                 saida.append("[" + exp.getText() + "]");
                }
            }
            saida.append(";\n");
        }
        return null;
    }

    @Override
    public Void visitTipo_estendido(LAParser.Tipo_estendidoContext ctx) {
        if (ctx.IDENT() != null) {
            String tipo = ctx.IDENT().getText();
            if (tabela.existeSimbolo(ctx.tipo_basico().IDENT(0).getText())) {
                tipo = tabela.verificar(ctx.tipo_basico().IDENT(1).getText()).tipo;
            }
            saida.append(tipo);
        } else {
            saida.append("void");
        }
        return null;
    }

    

}

