package com.baria.bot.handler;

import com.baria.bot.repository.ProductRepository;
import com.baria.bot.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import static org.junit.jupiter.api.Assertions.*;

public class CatalogHandlerTest {
    @Test
    public void testBrowse() {
        ProductRepository productRepository = Mockito.mock(ProductRepository.class);
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        CatalogHandler handler = new CatalogHandler(productRepository, userRepository);
        SendMessage msg = handler.handleCatalogBrowse(1L);
        assertTrue(msg.getText().contains("Выберите раздел"));
    }
}
