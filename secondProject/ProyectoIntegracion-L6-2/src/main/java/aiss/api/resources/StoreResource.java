package aiss.api.resources;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.spi.BadRequestException;
import org.jboss.resteasy.spi.NotFoundException;

import aiss.model.Game;
import aiss.model.StoreGame;
import aiss.model.Platform;
import aiss.model.Store;
import aiss.model.repository.DatabaseRepository;
import aiss.model.repository.MapDatabaseRepository;

@Path("/stores")
public class StoreResource {

	private static StoreResource _instance = null;
	DatabaseRepository repository;

	public StoreResource() {
		repository = MapDatabaseRepository.getInstance();
	}

	public static StoreResource getInstance() {
		if (_instance == null) {
			_instance = new StoreResource();
		}
		return _instance;
	}

	public Collection<Store> getAll() {
		return getAll(null, null, null, null, null, null, null, null);
	}

	@GET
	@Produces("application/json")
	public Collection<Store> getAll(@QueryParam("order") String order, @QueryParam("name") String name,
			@QueryParam("location") String location, @QueryParam("titleGame") String titleGame,
			@QueryParam("openHour") LocalTime openHour, @QueryParam("closeHour") LocalTime closeHour,
			@QueryParam("limit") Integer limit, @QueryParam("offset") Integer offset) {
		Stream<Store> result = repository.getAllStores().stream();
		if (!(name == null || !name.equals(""))) {
			result = result.filter(x -> x.getName().toLowerCase().contains(name.toLowerCase())
					|| x.getName().toLowerCase().equals(name.toLowerCase()));
		}
		if (!(location == null || !location.equals(""))) {
			result = result.filter(x -> x.getLocation().toLowerCase().contains(location.toLowerCase())
					|| x.getLocation().toLowerCase().equals(location.toLowerCase()));
		}

		if (!(titleGame == null || !titleGame.equals(""))) {
			List<Store> aux = new ArrayList<Store>();
			Boolean predicate = false;
			for (Store s : result.collect(Collectors.toList())) {
				for (StoreGame sg : s.getGames()) {
					if (sg.getGame().getTitle().toLowerCase().equals(titleGame.toLowerCase()))
						predicate = true;
				}
				if (predicate)
					aux.add(s);

			}
			result = aux.stream();
		}
		if (!(openHour.equals(null))) {
			result.filter(x -> x.getOpenHour().equals(openHour)).collect(Collectors.toList());
		}
		if (!(closeHour.equals(null))) {
			result.filter(x -> x.getCloseHour().equals(closeHour)).collect(Collectors.toList());
		}

		if (!(order == null || order.equals(""))) {
			Boolean noValido = false;
			switch (order) {
			default:
				noValido = true;
				break;
			case "name":
				result = result.sorted(Comparator.comparing(Store::getName));
				break;
			case "-name":
				result = result.sorted(Comparator.comparing(Store::getName).reversed());
				break;
			case "location":
				result = result.sorted(Comparator.comparing(Store::getLocation));
				break;
			case "-location":
				result = result.sorted(Comparator.comparing(Store::getLocation).reversed());
				break;
			case "openHour":
				result = result.sorted(Comparator.comparing(Store::getOpenHour));
				break;
			case "-openHour":
				result = result.sorted(Comparator.comparing(Store::getOpenHour).reversed());
				break;
			case "closeHour":
				result = result.sorted(Comparator.comparing(Store::getCloseHour));
				break;
			case "-closeHour":
				result = result.sorted(Comparator.comparing(Store::getCloseHour).reversed());
				break;
			}
			if (noValido) {
				throw new BadRequestException(
						"The format of the order parameter must be name, -name, year, -year, country, or -country");
			}
		}
		List<Store> res = result.collect(Collectors.toList());

		if (offset == null) {
			offset = 0;
		}

		if (limit == null || limit + offset > res.size()) {
			limit = res.size();
		}

		else {
			limit += offset;
		}

		return res.subList(offset, limit);

	}

	@GET
	@Path("/{id}")
	@Produces("application/json")
	public Store getStore(@PathParam("id") String id) {
		Store list = repository.getStore(id);

		if (list == null) {
			throw new NotFoundException("The store with id=" + id + " was not found");
		}

		return list;
	}
	
	@GET
	@Path("/store/cheapestGames")
	@Produces("aplication/json")
	public Collection<Store> getCheapestGamesInArea(@QueryParam("titleGame") String titleGame,
			@QueryParam("location") String location) {
		Collection<Store> stores = null;
		if (!titleGame.equals("") || !titleGame.equals(null)) {
			throw new BadRequestException("The title game must not be null");
		} else {
			stores = getAll(null, null, location, titleGame, null, null, null, null);
			List<Store> aux = new ArrayList<Store>();
			for(Store s: stores) {
				Boolean predicate = false;
				for(StoreGame sg: s.getGames()) {
					if(sg.getStock()>0)
						predicate=true;
				}
				aux.add(s);
			}
			stores = aux;

		}
		return stores;
	}

	@POST
	@Consumes("application/json")
	@Produces("application/json")
	public Response addStore(Store g) {
		if (g.getName() == null || "".equals(g.getName())) {
			throw new BadRequestException("The name of the store must not be null");
		}
		if (g.getCloseHour().equals(null)) {
			throw new BadRequestException("The close hour of the store must not be null");

		}
		if (g.getOpenHour().equals(null)) {
			throw new BadRequestException("The close hour of the store must not be null");

		}
		if (g.getGames().isEmpty() || g.getGames().equals(null)) {
			throw new BadRequestException("The games of the store must not be null");

		}
		if (g.getLocation().equals(null) || g.getLocation().equals("")) {
			throw new BadRequestException("The location of the store must not be null");

		}
		if (g.getPhone().equals(null) || g.getPhone().equals("")) {
			throw new BadRequestException("The phone of the store must not be null");

		}

		repository.addStore(g);
		return Response.noContent().build();
	}
	
	@PUT
	@Consumes("application/json")
	public Response updateStore(Store g) {
		Store old = repository.getStore(g.getId());
		if (old.getName() == null || "".equals(g.getName())) {
			throw new BadRequestException("The name of the store must not be null");
		}
		if (old.getCloseHour().equals(null)) {
			throw new BadRequestException("The close hour of the store must not be null");

		}
		if (old.getOpenHour().equals(null)) {
			throw new BadRequestException("The close hour of the store must not be null");

		}
		if (old.getGames().isEmpty() || old.getGames().equals(null)) {
			throw new BadRequestException("The games of the store must not be null");

		}
		if (old.getLocation().equals(null) || old.getLocation().equals("")) {
			throw new BadRequestException("The location of the store must not be null");

		}
		if (old.getPhone().equals(null) || old.getPhone().equals("")) {
			throw new BadRequestException("The phone of the store must not be null");

		}
		repository.updateStore(old);
		return Response.noContent().build();
	}

	@DELETE
	@Path("/{id}")
	public Response deleteStore(@PathParam("id") String id) {
		Store toRemove = repository.getStore(id);

		if (toRemove == null) {
			throw new NotFoundException("The store with id=" + id + " was not found");
		} else {
			repository.deleteStore(id);
		}
		return Response.noContent().build();
	}
	
	public void addStoreGame(String storeId, StoreGame game) {
		repository.addObjeto(storeId, game);
	}
	
	public Collection<StoreGame> getGamesFromStore(String storeId){
		return repository.getAllObjects(storeId);
	}
	
}
