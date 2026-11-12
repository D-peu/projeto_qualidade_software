package br.com.projeto_qualidade_software.projeto.exceptions;

public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}