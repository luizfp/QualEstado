package br.com.lfpmobile.qualoestado.database.interfaces;

import br.com.lfpmobile.qualoestado.dominio.Jogador;

/**
 * Created by luiz on 9/23/15.
 */
public interface JogadorDAO {

    Jogador inserirNovoJogador();
    Jogador getJogador();
    void atualizarNumAcertos(int numero);
    void atualizarNumErros(int numero);
    void atualizarNumPulosResposta(int numero);
    void atualizarNumUsosDicaBandeira(int numero);
    void atualizarNumUsosDicaDescricao(int numero);
    void atualizarNumUsosDicaLetra(int numero);
    void atualizarMaiorNumPontos(int numero);
    void atualizarMenorNumPontos(int numero);
}
