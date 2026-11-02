package com.controlefinanceiro.api.domain;
import com.controlefinanceiro.api.domain.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.math.BigDecimal;
import java.time.YearMonth;
import static org.junit.jupiter.api.Assertions.*;

class OrcamentoTest {
    private final Usuario dono = new Usuario("Ana", "ana@teste.com", "hash");
    private final Categoria despesa = new Categoria("Mercado", TipoCategoria.DESPESA, dono);
    private Dinheiro dinheiro(String v) { return new Dinheiro(new BigDecimal(v)); }
    @Test void deveValidarTipoDonoMesELimite() {
        assertThrows(RegraDeNegocioException.class, () -> new Orcamento(dono,
                new Categoria("Salário", TipoCategoria.RECEITA, dono), YearMonth.of(2026, 11), dinheiro("10")));
        assertThrows(RegraDeNegocioException.class, () -> new Orcamento(
                new Usuario("Beto", "beto@teste.com", "hash"), despesa, YearMonth.of(2026, 11), dinheiro("10")));
        assertThrows(RegraDeNegocioException.class, () -> new Orcamento(dono, despesa, null, dinheiro("10")));
        assertThrows(RegraDeNegocioException.class, () -> new Orcamento(dono, despesa, YearMonth.of(0, 1), dinheiro("10")));
        assertThrows(RegraDeNegocioException.class, () -> new Orcamento(dono, despesa, YearMonth.of(2026, 11), Dinheiro.ZERO));
        assertThrows(RegraDeNegocioException.class, () -> dinheiro("10.001"));
        var o = new Orcamento(dono, despesa, YearMonth.of(2026, 11), dinheiro("10"));
        o.alterarLimite(dinheiro("1206.35"));
        assertEquals(dinheiro("1206.35"), o.getLimite());
        assertEquals(YearMonth.of(2026, 11), o.getMes());
        assertThrows(RegraDeNegocioException.class, () -> o.alterarLimite(Dinheiro.ZERO));
    }
    @ParameterizedTest
    @CsvSource({"0,DENTRO_DO_LIMITE", "79.99,DENTRO_DO_LIMITE", "80,ATENCAO", "100,ATENCAO", "100.01,ACIMA_DO_LIMITE"})
    void situacaoDeveRespeitarAsFronteiras(String gasto, String situacao) {
        var consumo = ConsumoOrcamento.de(dinheiro("100"), new BigDecimal(gasto));
        assertEquals(situacao, consumo.situacao().name());
        assertEquals(0, new BigDecimal("100").subtract(new BigDecimal(gasto)).compareTo(consumo.restante()));
    }
    @Test void deveDecidirAntesDeArredondarENaoLimitarPercentual() {
        var quase = ConsumoOrcamento.de(dinheiro("100000"), new BigDecimal("79999.99"));
        assertEquals(ConsumoOrcamento.Situacao.DENTRO_DO_LIMITE, quase.situacao());
        var acima = ConsumoOrcamento.de(dinheiro("300"), new BigDecimal("350"));
        assertEquals(new BigDecimal("116.67"), acima.percentualConsumido());
        assertEquals(new BigDecimal("-50.00"), acima.restante());
    }
}
