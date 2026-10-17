package com.controlefinanceiro.api.domain;

import com.controlefinanceiro.api.domain.exception.RegraDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EstornoTest {

    private static final LocalDate DATA = LocalDate.of(2026, 10, 5);
    private static final LocalDate HOJE = LocalDate.of(2026, 10, 20);

    private Usuario usuario;
    private Conta conta;
    private Categoria categoriaReceita;
    private Categoria categoriaDespesa;

    private static Dinheiro reais(String valor) {
        return new Dinheiro(new BigDecimal(valor));
    }

    @BeforeEach
    void preparar() {
        usuario = new Usuario("Ana", "ana@teste.com", "hash");
        conta = new Conta("Nubank", usuario);
        categoriaReceita = new Categoria("Salário", TipoCategoria.RECEITA, usuario);
        categoriaDespesa = new Categoria("Alimentação", TipoCategoria.DESPESA, usuario);
    }

    @Test
    void deveEstornarDespesaCreditandoAConta() {
        conta.registrarReceita("Salário", reais("100"), DATA, categoriaReceita);
        Transacao despesa = conta.registrarDespesa("Mercado", reais("30"), DATA, categoriaDespesa);

        Transacao estorno = conta.estornar(despesa, HOJE);

        assertEquals(TipoTransacao.ESTORNO_DESPESA, estorno.getTipo());
        assertEquals(reais("30"), estorno.getValor());
        assertEquals("Estorno: Mercado", estorno.getDescricao());
        assertEquals(HOJE, estorno.getData());
        assertEquals(reais("100"), conta.getSaldo());
    }

    @Test
    void deveEstornarReceitaDebitandoAConta() {
        Transacao receita = conta.registrarReceita("Salário", reais("100"), DATA, categoriaReceita);

        Transacao estorno = conta.estornar(receita, HOJE);

        assertEquals(TipoTransacao.ESTORNO_RECEITA, estorno.getTipo());
        assertEquals(Dinheiro.ZERO, conta.getSaldo());
    }

    @Test
    void deveRecusarEstornoDeReceitaJaGastaSemAlterarOSaldo() {
        Transacao receita = conta.registrarReceita("Salário", reais("100"), DATA, categoriaReceita);
        conta.registrarDespesa("Mercado", reais("30"), DATA, categoriaDespesa);

        assertThrows(RegraDeNegocioException.class, () -> conta.estornar(receita, HOJE));

        assertEquals(reais("70"), conta.getSaldo());
    }

    @Test
    void deveRecusarEstornarTransferencia() {
        conta.creditar(reais("100"));
        Conta destino = new Conta("Itaú", usuario);
        Transacao saida = conta.transferirPara(destino, "Reserva", reais("40"), DATA).saida();

        assertThrows(RegraDeNegocioException.class, () -> conta.estornar(saida, HOJE));

        assertEquals(reais("60"), conta.getSaldo());
    }

    @Test
    void deveRecusarEstornarUmEstorno() {
        Transacao receita = conta.registrarReceita("Salário", reais("100"), DATA, categoriaReceita);
        Transacao estorno = conta.estornar(receita, HOJE);

        assertThrows(RegraDeNegocioException.class, () -> conta.estornar(estorno, HOJE));
    }

    @Test
    void deveRecusarTransacaoDeOutraConta() {
        Transacao receita = conta.registrarReceita("Salário", reais("100"), DATA, categoriaReceita);
        Conta outra = new Conta("Itaú", usuario);

        assertThrows(RegraDeNegocioException.class, () -> outra.estornar(receita, HOJE));
        assertThrows(RegraDeNegocioException.class,
                () -> outra.editarTransacao(receita, "Novo", categoriaReceita));
        assertEquals(reais("100"), conta.getSaldo());
    }

    @Test
    void deveEditarDescricaoECategoriaSemMexerNoSaldo() {
        Categoria outraReceita = new Categoria("Freelance", TipoCategoria.RECEITA, usuario);
        Transacao receita = conta.registrarReceita("Salário", reais("100"), DATA, categoriaReceita);

        conta.editarTransacao(receita, "  Pagamento extra  ", outraReceita);

        assertEquals("Pagamento extra", receita.getDescricao());
        assertEquals(outraReceita, receita.getCategoria());
        assertEquals(reais("100"), conta.getSaldo());
    }

    @Test
    void deveRecusarEdicaoComCategoriaDeTipoErradoOuDeTransferencia() {
        Transacao receita = conta.registrarReceita("Salário", reais("100"), DATA, categoriaReceita);

        assertThrows(RegraDeNegocioException.class,
                () -> conta.editarTransacao(receita, "Novo", categoriaDespesa));
        assertEquals("Salário", receita.getDescricao());

        Conta destino = new Conta("Itaú", usuario);
        Transacao saida = conta.transferirPara(destino, "Reserva", reais("40"), DATA).saida();
        assertThrows(RegraDeNegocioException.class,
                () -> conta.editarTransacao(saida, "Novo", categoriaReceita));
    }
}