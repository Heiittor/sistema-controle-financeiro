package com.controlefinanceiro.api.domain;

import com.controlefinanceiro.api.domain.exception.RegraDeNegocioException;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

@Entity
@Table(name = "orcamentos")
public class Orcamento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;
    @Column(nullable = false)
    private LocalDate mes;
    @Embedded
    @AttributeOverride(name = "valor", column = @Column(name = "limite", nullable = false, precision = 19, scale = 2))
    private Dinheiro limite;
    @Version
    private Long version;

    protected Orcamento() {}

    public Orcamento(Usuario usuario, Categoria categoria, YearMonth mes, Dinheiro limite) {
        if (usuario == null || categoria == null)
            throw new RegraDeNegocioException("Usuário e categoria são obrigatórios.");
        if (categoria.getUsuario() != usuario && (usuario.getId() == null ||
                !Objects.equals(categoria.getUsuario().getId(), usuario.getId())))
            throw new RegraDeNegocioException("A categoria deve pertencer ao usuário do orçamento.");
        if (categoria.getTipo() != TipoCategoria.DESPESA)
            throw new RegraDeNegocioException("O orçamento exige uma categoria de despesa.");
        validarMes(mes);
        this.usuario = usuario;
        this.categoria = categoria;
        this.mes = mes.atDay(1);
        alterarLimite(limite);
    }

    public static void validarMes(YearMonth mes) {
        if (mes == null || mes.getYear() < 1 || mes.getYear() > 9999)
            throw new RegraDeNegocioException("Informe um mês válido entre 0001-01 e 9999-12.");
    }

    public void alterarLimite(Dinheiro limite) {
        if (limite == null || limite.ehZero())
            throw new RegraDeNegocioException("O limite deve ser maior que zero.");
        if (limite.valor().precision() > 19)
            throw new RegraDeNegocioException("O limite deve ter no máximo 17 dígitos inteiros.");
        this.limite = limite;
    }
    public Long getId() { return id; }
    public Categoria getCategoria() { return categoria; }
    public YearMonth getMes() { return YearMonth.from(mes); }
    public Dinheiro getLimite() { return limite; }
}
