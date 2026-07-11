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
import com.site.toffCo.module.produto.service.ProdutoService;
import com.site.toffCo.module.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarrinhoService {

    private final CarrinhoRepository repository;
    private final CarrinhoMapper mapper;
    private final ProdutoRepository produtoRepository;
    private final AuthUtil authUtil;

    //============================== CREATE ==============================

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

    //============================== FINDCAR ==============================

    // guarda automaticamente no redis
    @Transactional(readOnly = true)
    @Cacheable(value = "carrinhos", key = "@authUtil.getUserLogado().id")
    public CarrinhoResponseDTO findByCar() {
        User user = authUtil.getUserLogado();
        Carrinho carrinho = repository.findByUser_Id(user.getId())
                .orElseThrow(() -> new CarNotFound("Carrinho nao encontrado"));
        return mapper.toDto(carrinho);
    }

    //============================== ADDITEM ==============================

    @Transactional
    @CacheEvict(value = "carrinhos", key = "@authUtil.getUserLogado().id") // limpa o cache
    public CarrinhoResponseDTO addItem(UUID produtoId, Integer quantidade) {
        if (quantidade == null || quantidade <= 0 || quantidade > 1000) {
            throw new QuantidadInvalid("Quantidade deve ser maior que zero");
        }
        Produto produtoEstoque = produtoRepository.findByIdForUpdate(produtoId)
                .orElseThrow(() -> new ProductNotFound("Produto nao encontrado"));
        //validação de estoque
        if(produtoEstoque.getEstoque() < quantidade) {
            log.warn("Estoque insuficiente para o produto: {}, novo estoque={}, solicitado={}",
                    produtoId, produtoEstoque.getEstoque(), quantidade);
            throw new QuantidadInvalid("Quantidade insuficiente para a compra");
        }

        produtoEstoque.setEstoque(produtoEstoque.getEstoque() - quantidade);
        produtoRepository.save(produtoEstoque);
        log.info("Estoque atualizado: produto={}, novo estoque={}",
                produtoId, produtoEstoque.getEstoque());
        Carrinho carrinho = getOuCreateCar();

        Produto produto = produtoEstoque;

        ItemCarrinho item = carrinho.getItens()
                .stream()
                .filter(i -> i.getProduto().getId().equals(produtoId))
                .findFirst()
                .orElseGet(() -> {
                    ItemCarrinho itemCarrinho = new ItemCarrinho();
                    itemCarrinho.setCarrinho(carrinho);
                    itemCarrinho.setProduto(produto);
                    itemCarrinho.setQuantidade(0);
                    carrinho.getItens().add(itemCarrinho);
                    return itemCarrinho;
                });

        item.setQuantidade(item.getQuantidade() + quantidade);
        item.setPrice(produto.getPrice());
        // 🔥 ADICIONE ISSO: Recalcula o valor total do carrinho somando os itens ativos
        BigDecimal total = carrinho.getItens().stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        carrinho.setValorTotal(total);

        log.info("Item adicionado ao carrinho: usuario={}, produto={}, quantidade={}",
                carrinho.getUser().getId(), produtoId, quantidade);
        return mapper.toDto(repository.save(carrinho));
    }

    //============================== REMOVE ==============================

    @Transactional
    @CacheEvict(value = "carrinhos", key = "@authUtil.getUserLogado().id")
    public void removerItem(UUID itemId) {
        User usuario = authUtil.getUserLogado();
        Carrinho carrinho = repository.findByUser_Id(usuario.getId())
                .orElseThrow(() -> new CarNotFound("Carrinho não encontrado"));
        // 1. Encontra o item
        ItemCarrinho itemParaRemover = carrinho.getItens().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ItemNotFound("Item não encontrado no carrinho"));

        // 2. DEVOLVE O ESTOQUE (Pessimistic Lock aqui também para segurança)
        Produto produto = produtoRepository.findByIdForUpdate(itemParaRemover.getProduto().getId())
                .orElseThrow(() -> new ProductNotFound("Produto não encontrado"));

        produto.setEstoque(produto.getEstoque() + itemParaRemover.getQuantidade());
        produtoRepository.save(produto);

        log.info("Estoque devolvido: produto={}, quantidade devolvida={}, novo estoque={}",
                produto.getId(), itemParaRemover.getQuantidade(), produto.getEstoque());
        // 3. Remove do carrinho
        carrinho.getItens().remove(itemParaRemover);
        repository.save(carrinho);
        log.info("Item removido do carrinho: usario={}, item={}",
                usuario.getId(), itemId);
    }

    @Transactional
    @CacheEvict(value = "carrinhos", key = "@authUtil.getUserLogado().id")
    public CarrinhoResponseDTO updateCar(CarrinhoRequestDTO carrinhoRequestDTO) {
        UUID carrinhoId = carrinhoRequestDTO.produtoId();
        Integer novaQuantidade = carrinhoRequestDTO.quantidade();

        if (novaQuantidade == null || novaQuantidade < 0 || novaQuantidade > 1000) {
            throw new QuantidadInvalid("Quantidade do produto invalida");
        }

        User usuario = authUtil.getUserLogado();
        Carrinho carrinho = repository.findByUser_Id(usuario.getId()).orElseThrow(
                () -> new CarNotFound("Carrinho vazio")
        );

        ItemCarrinho item = carrinho.getItens().stream()
                .filter(i -> i.getProduto().getId().equals(carrinhoId))
                .findFirst()
                .orElseThrow(() -> new ItemNotFound("Item not found"));

        if (novaQuantidade == 0) {
            removerItem(item.getId());
            return findByCar();
        }

        int quantidadeAtual = item.getQuantidade();
        int diferenca =  novaQuantidade - quantidadeAtual;

        if (diferenca == 0) {
            return mapper.toDto(carrinho);
        }

        Produto produtoEstoque = produtoRepository.findByIdForUpdate(carrinhoId).orElseThrow(
                () -> new ProductNotFound("Produto nao encontrado")
        );

        if (diferenca > 0) {
            if (produtoEstoque.getEstoque() < diferenca) {
                throw new QuantidadInvalid("Quantidade do produto invalida");
            }
            produtoEstoque.setEstoque(produtoEstoque.getEstoque() - diferenca);
        } else {
            produtoEstoque.setEstoque(produtoEstoque.getEstoque() + Math.abs(diferenca));
        }

        produtoRepository.save(produtoEstoque);

        item.setQuantidade(novaQuantidade);
        item.setPrice(produtoEstoque.getPrice());

        BigDecimal total = carrinho.getItens().stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        carrinho.setValorTotal(total);

        log.info("Carrinho atualizado: usuario={}, produto={}, quantidade antiga={}, nova quantidade={}",
                usuario.getId(), carrinhoId, quantidadeAtual, novaQuantidade);

        return mapper.toDto(repository.save(carrinho));
    }
}
