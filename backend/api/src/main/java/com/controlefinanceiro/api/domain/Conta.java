package com.controlefinanceiro.api.domain;

import com.controlefinanceiro.api.domain.exception.RegraDeNegocioException;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "contas")
public class Conta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Embedded
    @AttributeOverride(name = "valor",
            column = @Column(name = "saldo", nullable = false, precision = 19, scale = 2))
    private Dinheiro saldo;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "criado_em", insertable = false, updatable = false)
    private OffsetDateTime criadoEm;

    // Exigido pelo JPA.
    protected Conta() {
    }

    public Conta(String nome, Usuario usuario) {
        this.nome = normalizarNome(nome);
        if (usuario == null) {
            throw new RegraDeNegocioException("O usuário da conta é obrigatório.");
        }
        this.usuario = usuario;
        this.saldo = Dinheiro.ZERO;
    }

    public void creditar(Dinheiro valor) {
        exigirValorPositivo(valor);
        this.saldo = this.saldo.somar(valor);
    }

    public void debitar(Dinheiro valor) {
        exigirValorPositivo(valor);
        if (valor.maiorQue(this.saldo)) {
            // IMPORTANTE: é aqui que mora a regra do saldo insuficiente. Nada foi alterado ainda.
            throw new RegraDeNegocioException("Saldo insuficiente.");
        }
        this.saldo = this.saldo.subtrair(valor);
    }

    public Transacao registrarReceita(String descricao, Dinheiro valor, LocalDate data, Categoria categoria) {
        Transacao transacao = new Transacao(this, TipoTransacao.RECEITA, descricao, valor, data, categoria);
        creditar(valor);
        return transacao;
    }

    public Transacao registrarDespesa(String descricao, Dinheiro valor, LocalDate data, Categoria categoria) {
        Transacao transacao = new Transacao(this, TipoTransacao.DESPESA, descricao, valor, data, categoria);
        debitar(valor);
        return transacao;
    }

    // As duas pontas de uma transferência.
    public record ParTransferencia(Transacao saida, Transacao entrada) {
    }

    public ParTransferencia transferirPara(Conta destino, String descricao, Dinheiro valor, LocalDate data) {
        if (destino == null) {
            throw new RegraDeNegocioException("A conta de destino é obrigatória.");
        }
        // IMPORTANTE: transferir para si mesma não faz sentido e geraria dois lançamentos inúteis.
        if (this.mesmaContaQue(destino)) {
            throw new RegraDeNegocioException("A conta de origem e a de destino devem ser diferentes.");
        }
        // Segunda linha de defesa: o Service já busca as duas contas com o dono.
        if (!Objects.equals(this.usuario.getId(), destino.usuario.getId())) {
            throw new RegraDeNegocioException("Só é possível transferir entre contas do mesmo usuário.");
        }

        UUID transferenciaId = UUID.randomUUID();

        // Cria as duas transações ANTES de mexer nos saldos: se algo for inválido, nada foi alterado.
        Transacao saida = new Transacao(this, TipoTransacao.TRANSFERENCIA_SAIDA,
                descricao, valor, data, transferenciaId);
        Transacao entrada = new Transacao(destino, TipoTransacao.TRANSFERENCIA_ENTRADA,
                descricao, valor, data, transferenciaId);

        // Débito primeiro: se faltar saldo, lança "Saldo insuficiente." sem ter creditado nada.
        this.debitar(valor);
        destino.creditar(valor);

        return new ParTransferencia(saida, entrada);
    }

    // Desfaz uma receita ou despesa com um lançamento inverso. Nada é apagado: o histórico é imutável.
    public Transacao estornar(Transacao original, LocalDate data) {
        exigirTransacaoDestaConta(original);

        // Cria primeiro: a Transacao recusa transferência e estorno, antes de qualquer mudança no saldo.
        Transacao estorno = new Transacao(original, data);

        // Estornar receita tira dinheiro (pode faltar saldo); estornar despesa devolve.
        if (original.getTipo() == TipoTransacao.RECEITA) {
            debitar(original.getValor());
        } else {
            creditar(original.getValor());
        }
        return estorno;
    }

    // Edita só descrição e categoria. Não mexe no saldo: valor e data são fatos financeiros.
    public void editarTransacao(Transacao transacao, String novaDescricao, Categoria novaCategoria) {
        exigirTransacaoDestaConta(transacao);
        transacao.editar(novaDescricao, novaCategoria);
    }

    public void renomear(String novoNome) {
        this.nome = normalizarNome(novoNome);
    }

    private void exigirTransacaoDestaConta(Transacao transacao) {
        if (transacao == null) {
            throw new RegraDeNegocioException("A transação é obrigatória.");
        }
        if (!this.mesmaContaQue(transacao.getConta())) {
            throw new RegraDeNegocioException("A transação não pertence a esta conta.");
        }
    }

    // Compara por id quando ambos existem; sem id (só em testes), pela referência do objeto.
    private boolean mesmaContaQue(Conta outra) {
        if (outra == null) {
            return false;
        }
        return (this.id != null && outra.id != null)
                ? this.id.equals(outra.id)
                : this == outra;
    }

    private static void exigirValorPositivo(Dinheiro valor) {
        if (valor == null || valor.ehZero()) {
            throw new RegraDeNegocioException("O valor da operação deve ser maior que zero.");
        }
    }

    private static String normalizarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new RegraDeNegocioException("O nome da conta é obrigatório.");
        }
        String limpo = nome.trim();
        if (limpo.length() > 100) {
            throw new RegraDeNegocioException("O nome da conta deve ter no máximo 100 caracteres.");
        }
        return limpo;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public Dinheiro getSaldo() { return saldo; }
    public Usuario getUsuario() { return usuario; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
}