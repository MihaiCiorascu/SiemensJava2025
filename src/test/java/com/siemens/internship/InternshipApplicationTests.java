package com.siemens.internship;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class InternshipApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ItemService itemService;

	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();
	}

	@Test
	void testCreateItemValid() throws Exception {
		Item item = new Item(1L, "Item Name", "Item Description", "PENDING", "test@example.com");
		when(itemService.save(any(Item.class))).thenReturn(item);

		mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(item)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Item Name"));
	}

	@Test
	void testCreateItemInvalidEmail() throws Exception {
		Item item = new Item(1L, "Item Name", "Item Description", "PENDING", "invalid-email");

		mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(item)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void testGetItemById() throws Exception {
		Item item = new Item(1L, "Item Name", "Item Description", "PROCESSED", "test@example.com");
		when(itemService.findById(1L)).thenReturn(Optional.of(item));

		mockMvc.perform(get("/api/items/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.name").value("Item Name"))
				.andExpect(jsonPath("$.description").value("Item Description"))
				.andExpect(jsonPath("$.status").value("PROCESSED"))
				.andExpect(jsonPath("$.email").value("test@example.com"));

		verify(itemService).findById(1L);
	}

	@Test
	void testGetItemByIdNotFound() throws Exception {
		when(itemService.findById(999L)).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/items/999"))
				.andExpect(status().isNotFound());
	}

	@Test
	void testUpdateItem() throws Exception {
		Item existingItem = new Item(1L, "Existing Item", "Existing Description", "PENDING", "test@example.com");
		Item updatedItem = new Item(1L, "Updated Item", "Updated Description", "PROCESSED", "test@example.com");

		when(itemService.findById(1L)).thenReturn(Optional.of(existingItem));
		when(itemService.save(any(Item.class))).thenReturn(updatedItem);

		mockMvc.perform(put("/api/items/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(updatedItem)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Updated Item"));
	}

	@Test
	void testUpdateItemNotFound() throws Exception {
		Item updatedItem = new Item(1L, "Updated Item", "Updated Description", "PROCESSED", "test@example.com");
		when(itemService.findById(1L)).thenReturn(Optional.empty());

		mockMvc.perform(put("/api/items/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(updatedItem)))
				.andExpect(status().isNotFound());
	}

	@Test
	void testDeleteItem() throws Exception {
		Item item = new Item(1L, "Item Name", "Item Description", "PENDING", "test@example.com");
		when(itemService.findById(1L)).thenReturn(Optional.of(item));
		doNothing().when(itemService).deleteById(1L);

		mockMvc.perform(delete("/api/items/1"))
				.andExpect(status().isNoContent());
	}

	@Test
	void testDeleteItemNotFound() throws Exception {
		when(itemService.findById(999L)).thenReturn(Optional.empty());

		mockMvc.perform(delete("/api/items/999"))
				.andExpect(status().isNotFound());
	}

	@Test
	void testProcessItems() throws Exception {
		Item processedItem = new Item(1L, "Processed Item", "Processed Description", "PROCESSED", "test@example.com");
		CompletableFuture<List<Item>> future = CompletableFuture.completedFuture(List.of(processedItem));
		
		when(itemService.processItemsAsync()).thenReturn(future);

		mockMvc.perform(get("/api/items/process"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1))
				.andExpect(jsonPath("$[0].name").value("Processed Item"))
				.andExpect(jsonPath("$[0].status").value("PROCESSED"));
	}
}
