package com.site.toffCo.module.carrinho.service;

import com.site.toffCo.infra.exception.carrinho.CarNotFound;
import com.site.toffCo.infra.exception.item.ItemNotFound;
import com.site.toffCo.infra.exception.item.QuantidadInvalid;
import com.site.toffCo.infra.exception.product.ProductNotFound;
import com.site.toffCo.infra.utils.AuthUtil;
import com.site.toffCo.module.carrinho.dto.CarrinhoRequestDTO;
import com.site.toffCo.module.carrinho.dto.CarrinhoResponseDTO;
import com.site.toffCo.module.carrinho.entity.Carrinho;
import com.site.toffCo.module.carrinho.mapper.CarrinhoMapper;
import com.site.toffCo.module.carrinho.repository.CarrinhoRepository;
import com.site.toffCo.module.itemcarrinho.entity.ItemCarrinho;
import com.site.toffCo.module.produto.entity.Produto;
import com.site.toffCo.module.produto.repository.ProdutoRepository;
import com.site.toffCo.module.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarrinhoService {

    private static final BigDecimal QUANTIDADE_MAXIMA =
            BigDecimal.valueOf(1000);

    private final CarrinhoRepository repository;
    private final CarrinhoMapper mapper;
    private final ProdutoRepository produtoRepository;
    private final AuthUtil authUtil;

    // ============================== CREATE ==============================

    @Transactional
    public Carrinho getOuCreateCar() {
        User user = authUtil.getUserLogado();

        return repository.findByUser_Id(user.getId())
                .orElseGet(() -> {
                    Carrinho carrinho = new Carrinho();
                    carrinho.setUser(user);

                    return repository.save(carrinho);
                });
    }

    // ============================== FIND CAR ==============================

    @Transactional(readOnly = true)
    @Cacheable(
            value = "carrinhos",
            key = "@authUtil.getUserLogado().id"
    )
    public CarrinhoResponseDTO findByCar() {
        User user = authUtil.getUserLogado();

        Carrinho carrinho = repository.findByUser_Id(user.getId())
                .orElseThrow(() ->
                        new CarNotFound("Carrinho não encontrado")
                );

        return mapper.toDto(carrinho);
    }

    // ============================== ADD ITEM ==============================

    @Transactional
    @CacheEvict(
            value = "carrinhos",
            key = "@authUtil.getUserLogado().id"
    )
    public CarrinhoResponseDTO addItem(
            UUID produtoId,
            BigDecimal quantidade
    ) {
        validarQuantidade(quantidade);

        Produto produto = produtoRepository.findByIdForUpdate(produtoId)
                .orElseThrow(() ->
                        new ProductNotFound("Produto não encontrado")
                );

        if (produto.getEstoque().compareTo(quantidade) < 0) {
            log.warn(
                    "Estoque insuficiente: produto={}, estoque={}, solicitado={}",
                    produtoId,
                    produto.getEstoque(),
                    quantidade
            );

            throw new QuantidadInvalid(
                    "Quantidade insuficiente em estoque"
            );
        }

        produto.setEstoque(
                produto.getEstoque().subtract(quantidade)
        );

        produtoRepository.save(produto);

        log.info(
                "Estoque atualizado: produto={}, novo estoque={}",
                produtoId,
                produto.getEstoque()
        );

        Carrinho carrinho = getOuCreateCar();

        ItemCarrinho item = carrinho.getItens()
                .stream()
                .filter(existingItem ->
                        existingItem.getProduto()
                                .getId()
                                .equals(produtoId)
                )
                .findFirst()
                .orElseGet(() -> {
                    ItemCarrinho novoItem = new ItemCarrinho();

                    novoItem.setCarrinho(carrinho);
                    novoItem.setProduto(produto);
                    novoItem.setQuantidade(0);
                    novoItem.setPrice(produto.getPrice());

                    carrinho.getItens().add(novoItem);

                    return novoItem;
                });

        int quantidadeInteira;

        try {
            quantidadeInteira = quantidade.intValueExact();
        } catch (ArithmeticException exception) {
            throw new QuantidadInvalid(
                    "A quantidade deve ser um número inteiro"
            );
        }

        item.setQuantidade(
                item.getQuantidade() + quantidadeInteira
        );

        item.setPrice(produto.getPrice());

        recalcularValorTotal(carrinho);

        Carrinho carrinhoSalvo = repository.save(carrinho);

        log.info(
                "Item adicionado ao carrinho: usuario={}, produto={}, quantidade={}",
                carrinho.getUser().getId(),
                produtoId,
                quantidadeInteira
        );

        return mapper.toDto(carrinhoSalvo);
    }

    // ============================== REMOVE ITEM ==============================

    @Transactional
    @CacheEvict(
            value = "carrinhos",
            key = "@authUtil.getUserLogado().id"
    )
    public void removerItem(UUID itemId) {
        User usuario = authUtil.getUserLogado();

        Carrinho carrinho = repository.findByUser_Id(usuario.getId())
                .orElseThrow(() ->
                        new CarNotFound("Carrinho não encontrado")
                );

        ItemCarrinho itemParaRemover = carrinho.getItens()
                .stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() ->
                        new ItemNotFound(
                                "Item não encontrado no carrinho"
                        )
                );

        Produto produto = produtoRepository.findByIdForUpdate(
                        itemParaRemover.getProduto().getId()
                )
                .orElseThrow(() ->
                        new ProductNotFound("Produto não encontrado")
                );

        BigDecimal quantidadeDevolvida =
                BigDecimal.valueOf(itemParaRemover.getQuantidade());

        produto.setEstoque(
                produto.getEstoque().add(quantidadeDevolvida)
        );

        produtoRepository.save(produto);

        carrinho.getItens().remove(itemParaRemover);

        recalcularValorTotal(carrinho);

        repository.save(carrinho);

        log.info(
                "Item removido: usuario={}, item={}, produto={}, quantidade devolvida={}, novo estoque={}",
                usuario.getId(),
                itemId,
                produto.getId(),
                quantidadeDevolvida,
                produto.getEstoque()
        );
    }

    // ============================== UPDATE CAR ==============================

    @Transactional
    @CacheEvict(
            value = "carrinhos",
            key = "@authUtil.getUserLogado().id"
    )
    public CarrinhoResponseDTO updateCar(
            CarrinhoRequestDTO request
    ) {
        UUID produtoId = request.produtoId();
        Integer novaQuantidade = request.quantidade();

        if (novaQuantidade == null
                || novaQuantidade < 0
                || novaQuantidade > 1000) {

            throw new QuantidadInvalid(
                    "Quantidade deve estar entre 0 e 1000"
            );
        }

        User usuario = authUtil.getUserLogado();

        Carrinho carrinho = repository.findByUser_Id(usuario.getId())
                .orElseThrow(() ->
                        new CarNotFound("Carrinho vazio")
                );

        ItemCarrinho item = carrinho.getItens()
                .stream()
                .filter(existingItem ->
                        existingItem.getProduto()
                                .getId()
                                .equals(produtoId)
                )
                .findFirst()
                .orElseThrow(() ->
                        new ItemNotFound(
                                "Item não encontrado no carrinho"
                        )
                );

        if (novaQuantidade == 0) {
            devolverEstoqueERemoverItem(carrinho, item);

            Carrinho carrinhoSalvo = repository.save(carrinho);

            return mapper.toDto(carrinhoSalvo);
        }

        int quantidadeAtual = item.getQuantidade();
        int diferenca = novaQuantidade - quantidadeAtual;

        if (diferenca == 0) {
            return mapper.toDto(carrinho);
        }

        Produto produto = produtoRepository.findByIdForUpdate(produtoId)
                .orElseThrow(() ->
                        new ProductNotFound("Produto não encontrado")
                );

        if (diferenca > 0) {
            BigDecimal quantidadeAdicional =
                    BigDecimal.valueOf(diferenca);

            if (produto.getEstoque()
                    .compareTo(quantidadeAdicional) < 0) {

                throw new QuantidadInvalid(
                        "Quantidade insuficiente em estoque"
                );
            }

            produto.setEstoque(
                    produto.getEstoque()
                            .subtract(quantidadeAdicional)
            );

        } else {
            BigDecimal quantidadeDevolvida =
                    BigDecimal.valueOf(Math.abs(diferenca));

            produto.setEstoque(
                    produto.getEstoque()
                            .add(quantidadeDevolvida)
            );
        }

        produtoRepository.save(produto);

        item.setQuantidade(novaQuantidade);
        item.setPrice(produto.getPrice());

        recalcularValorTotal(carrinho);

        Carrinho carrinhoSalvo = repository.save(carrinho);

        log.info(
                "Carrinho atualizado: usuario={}, produto={}, quantidade antiga={}, nova quantidade={}, estoque={}",
                usuario.getId(),
                produtoId,
                quantidadeAtual,
                novaQuantidade,
                produto.getEstoque()
        );

        return mapper.toDto(carrinhoSalvo);
    }

    // ============================== AUXILIARES ==============================

    private void validarQuantidade(BigDecimal quantidade) {
        if (quantidade == null
                || quantidade.compareTo(BigDecimal.ZERO) <= 0
                || quantidade.compareTo(QUANTIDADE_MAXIMA) > 0) {

            throw new QuantidadInvalid(
                    "Quantidade deve ser maior que zero e menor ou igual a 1000"
            );
        }

        if (quantidade.stripTrailingZeros().scale() > 0) {
            throw new QuantidadInvalid(
                    "A quantidade deve ser um número inteiro"
            );
        }
    }

    private void recalcularValorTotal(Carrinho carrinho) {
        BigDecimal total = carrinho.getItens()
                .stream()
                .map(item ->
                        item.getPrice().multiply(
                                BigDecimal.valueOf(
                                        item.getQuantidade()
                                )
                        )
                )
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );

        carrinho.setValorTotal(total);
    }

    private void devolverEstoqueERemoverItem(
            Carrinho carrinho,
            ItemCarrinho item
    ) {
        Produto produto = produtoRepository.findByIdForUpdate(
                        item.getProduto().getId()
                )
                .orElseThrow(() ->
                        new ProductNotFound("Produto não encontrado")
                );

        BigDecimal quantidadeDevolvida =
                BigDecimal.valueOf(item.getQuantidade());

        produto.setEstoque(
                produto.getEstoque().add(quantidadeDevolvida)
        );

        produtoRepository.save(produto);

        carrinho.getItens().remove(item);

        recalcularValorTotal(carrinho);

        log.info(
                "Item removido ao zerar quantidade: produto={}, quantidade devolvida={}, estoque={}",
                produto.getId(),
                quantidadeDevolvida,
                produto.getEstoque()
        );
    }
}