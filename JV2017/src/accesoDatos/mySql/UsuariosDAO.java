/** 
 * Proyecto: Juego de la vida.
 * Resuelve todos los aspectos del almacenamiento del DTO Usuario 
 * utilizando base de datos mySQL
 * Colabora en el patron Fachada.
 * @since: prototipo2.2
 * @source: UsuariosDAO.java 
 * @version: 2.2 - 2018/06/07 
 * @author: Francisco, Gonzalo, Alejandro
 */

package accesoDatos.mySql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.swing.table.DefaultTableModel;

import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.query.Query;

import accesoDatos.DatosException;
import accesoDatos.OperacionesDAO;
import config.Configuracion;
import modelo.ClaveAcceso;
import modelo.Correo;
import modelo.DireccionPostal;
import modelo.ModeloException;
import modelo.Nif;
import modelo.Simulacion;
import modelo.Usuario;
import modelo.Usuario.RolUsuario;
import util.Fecha;

public class UsuariosDAO  implements OperacionesDAO {

	// Requerido por el Singleton. 
	private static UsuariosDAO instancia = null;
	private Connection db;

	private Statement sentenciaUsr;
	private Statement sentenciaId;
	private ResultSet rsUsuarios;
	private DefaultTableModel tmUsuarios;
	private ArrayList<Object> bufferObjetos;

	/**
	 * Constructor por defecto de uso interno.
	 * Sólo se ejecutará una vez.
	 */
	private UsuariosDAO() throws SQLException, DatosException {
		inicializar();
		if(obtener("III1R")== null) {
			cargarPredeterminados();
		}
	}

	/**
	 *  Método estático de acceso a la instancia única.
	 *  Si no existe la crea invocando al constructor interno.
	 *  Utiliza inicialización diferida.
	 *  Sólo se crea una vez; instancia única -patrón singleton-
	 *  @return instancia
	 */
	public static UsuariosDAO getInstancia() {
		if (instancia == null) {
			try {
				instancia = new UsuariosDAO();
			}
			catch (SQLException | DatosException e) {
				e.printStackTrace();
			}
		}
		return instancia;
	}
	/**
	 *  Inicializa el DAO, detecta si existen las tablas de datos capturar
	 *  excepcion SQLException
	 *  @throws SQLException
	 */

	private void inicializar() throws SQLException{
		bufferObjetos = new ArrayList<Object>();
		db = Conexion.getDb();
		try {
			//Creamos dos Statements en la conexion a la BD para las consultas
			sentenciaUsr = db.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			sentenciaId = db.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			sentenciaUsr.executeQuery("SELECT * FROM usuarios");
			sentenciaId.executeQuery("SELECT * FROM equivalid");
		}
		catch(SQLException e) {
			crearTablaUsuarios();
			crearTablaEquivalId();
		}
		//Crea el tablemodel y el buffer de objetos para Usuarios.
		tmUsuarios = new DefaultTableModel();
		bufferObjetos = new ArrayList<Object>();
	}

	/**
	 *  Método para crear la tabla usuarios en la base de datos
	 *  @throws SQLException
	 */
	private void crearTablaUsuarios() throws SQLException {
		//Realizamos la conexión a la BD.
		Statement s = db.createStatement();

		//Creamos la tabla Usuarios
		s.executeUpdate("CREATE TABLE usuarios ("
				+ "idUsr VARCHAR(5) NOT NULL,"
				+ "NIF VARCHAR(9) NOT NULL,"
				+ "Nombre VARCHAR(50) NOT NULL,"
				+ "Apellidos VARCHAR(100) NOT NULL,"
				+ "Calle VARCHAR(50) NOT NULL,"
				+ "Numero VARCHAR(5) NOT NULL,"
				+ "CP INT(5) NOT NULL,"
				+ "Poblacion VARCHAR(50) NOT NULL,"
				+ "Correo VARCHAR(100) NOT NULL,"
				+ "FechaNacimiento DATE NOT NULL,"
				+ "FechaAlta DATE NOT NULL,"
				+ "ClaveAcceso VARCHAR(20) NOT NULL,"
				+ "Rol VARCHAR(20) NOT NULL,"
				+ "PRIMARY KEY(IdUsr));");
	}

	/**
	 *  Método para crear la tabla de equivalencia en la base de datos
	 *  @throws SQLException
	 */

	private void crearTablaEquivalId() throws SQLException {
		Statement s = db.createStatement();

		s.executeUpdate("CREATE TABLE equivalid ("
				+ "equival VARCHAR(50) NOT NULL,"
				+ "IdUsr VARCHAR(5) NOT NULL,"
				+ "PRIMARY KEY(equival));");
	}
	/**
	 *  Método para generar datos predeterminados.
	 */
	private void cargarPredeterminados() throws SQLException, DatosException {
	try {
			String nombreUsr = Configuracion.get().getProperty("usuario.admin");
			String password = Configuracion.get().getProperty("usuario.passwordPredeterminada");	
			Usuario usrPredeterminado = new Usuario(new Nif("00000000T"), nombreUsr, "Admin Admin", 
					new DireccionPostal("Iglesia", "00", "30012", "Murcia"), 
					new Correo("jv.admin" + "@gmail.com"), new Fecha(2000, 01, 01), 
					new Fecha(2005, 05, 05), new ClaveAcceso(password), RolUsuario.ADMINISTRADOR);
			alta(usrPredeterminado);

			nombreUsr = Configuracion.get().getProperty("usuario.invitado");
			usrPredeterminado = new Usuario(new Nif("00000001R"), nombreUsr, "Invitado Invitado", 
					new DireccionPostal("Iglesia", "00", "30012", "Murcia"), 
					new Correo("jv.invitado" + "@gmail.com"), new Fecha(2000, 01, 01), 
					new Fecha(2005, 05, 05), new ClaveAcceso(password), RolUsuario.INVITADO);
			alta(usrPredeterminado);
		} 
		catch (ModeloException e) { e.printStackTrace();
		}
	}

	/**
	 *  Cierra conexión.
	 */
	@Override
	public void cerrar() {
		try {
			sentenciaUsr.close();
			sentenciaId.close();
		}
		catch(SQLException e) {
			e.printStackTrace();
		}
	}

	//OPERACIONES DAO
	/**
	 * Obtiene un usuario dado su idUsr, el correo o su nif.
	 * @param id - el id de Usuario a buscar.
	 * @return - el Usuario encontrado. 
	 * @throws DatosException - si no existe.
	 */
	@Override
	public Usuario obtener(String id) throws DatosException {
		try {
			// AQU�
			rsUsuarios = sentenciaUsr.executeQuery("SELECT * FROM usuarios WHERE IdUsr = " + idUsr + "");
			//Establece columnas de filas. 
			estableceColumnasModelo();

			//Borrado previo de filas
			borraFilasModelo();

			//Volcado desde el resulSet 
			rellenaFilasModelo();

			//Actualiza buffer de objetos.
			sincronizaBufferObjetos();
			if (bufferObjetos.size() > 0) {
				return(Usuario) bufferObjetos.get(0);
			}
		}
		catch (SQLException e) {
			thows new DatosException("(OBTENER) El usuario: " + idUsr + "no exite.");
		}
		return null;
	}


	/**
	 * Obtiene todos los usuarios almacenados.
	 * @return - la List con todos los usuarios.
	 */
	@Override
	public List <Usuario> obtenerTodos() {
		Query consulta = db.query();
		consulta.constrain(Usuario.class);
		return consulta.execute();
	}

	/**
	 * Búsqueda de Usuario dado un objeto, reenvía al método que utiliza idUsr.
	 * @param obj - el Usuario a buscar.
	 * @return - el Usuario encontrado.
	 * @throws DatosException - si no existe.
	 */
	@Override
	public Usuario obtener(Object obj) throws DatosException  {
		return this.obtener(((Usuario) obj).getIdUsr());
	}	

	/**
	 *  Alta de un nuevo usuario sin repeticiones según el campo idUsr. 
	 *	@param obj - Objeto a almacenar.
	 *  @throws DatosException si ya existe y no ha podido generar variante.
	 */
	@Override
	public void alta(Object obj) throws DatosException  {

		Usuario usrNuevo = (Usuario) obj;
		Usuario usrPrevio = obtener(usrNuevo.getIdUsr());
		if(usrPrevio == null){
			try {
				almacenar(usrNuevo);
				registrarEquivalenciaId(usrNuevo);

			}
			catch (SQLException e) {
				e.printStackTrace();
			}
		}
		else{
			boolean condicion = !(usrNuevo.getCorreo().equals(usrPrevio.getCorreo())
					|| usrNuevo.getNif().equals(usrPrevio.getNif()));
			if(condicion) {
				int intentos = "ABCDEFGHIJKLMNPQRSTUVWXYZ".length();
				do {
					usrNuevo.generarVarianteIdUsr();
					usrPrevio = obtener(usrNuevo.getIdUsr());
					if(usrPrevio == null) {
						try {
							almacenar(usrNuevo);
							registrarEquivalenciaId(usrNuevo);
							return;
						}
						catch (SQLException e) {
							e.printStackTrace();
						}
					}
					intentos--;
				} while (intentos > 0);
			}
			throw new DatosException("(ALTA) El Usuario: "+ usrNuevo.getIdUsr() + " ya existe...");

		}

	}


	/**
	 * Genera variante de IdUsr cuando se produce coincidencia de identificador 
	 * con un usuario ya almacenado. 
	 * @param usrNuevo
	 * @param usrPrevio
	 * @throws DatosException si no puede generar variante de idUsr. 
	 */
	private void generarVarianteIdUsr(Usuario usrNuevo, Usuario usrPrevio) throws DatosException {
		// Comprueba que no haya coincidencia de Correo y Nif (ya existe)
		boolean condicion = !(usrNuevo.getCorreo().equals(usrPrevio.getCorreo())
				|| usrNuevo.getNif().equals(usrPrevio.getNif()));
		if (condicion) {
			int intentos = "ABCDEFGHJKLMNPQRSTUVWXYZ".length();				// 24 letras
			do {
				try {
					usrNuevo.generarVarianteIdUsr();
					usrPrevio = obtener(usrNuevo.getIdUsr());
				}
				catch (DatosException e) {
					return;
				}
				intentos--;
			} while (intentos > 0);
			throw new DatosException("Variar idUsr: " + usrNuevo.getIdUsr() + " imposible generar variante.");
		}
	}

	/**
	 * Elimina el objeto, dado el id utilizado para el almacenamiento.
	 * @param id - el identificador del objeto a eliminar.
	 * @return - el Objeto eliminado. 
	 * @throws DatosException - si no existe.
	 */
	@Override
	public Object baja(String idUsr) throws DatosException {
		Usuario usr = obtener(idUsr);
		if(usr != null) {
			try {
				borrarEquivalenciaId(usr.getIdUsr());
				borrar(usr);
			}
			catch (SQLException e) {
				e.printStackTrace();
			}
		}
		else {
			throw new DatosException("Baja: "+ idUsr + " no existe.");
		}
		return usr; 
	}

	/**
	 *  Actualiza datos de un Usuario reemplazando el almacenado por el recibido. 
	 *  No admitirá cambios en el idUsr.
	 *	@param obj - Usuario con los cambios.
	 * @throws DatosException - si no existe.
	 */
	@Override
	public void actualizar(Object obj) throws DatosException  {
		assert obj != null;
		Usuario usrActualizado = (Usuario) obj;
		Usuario usrPrevio = null;
		try {
			usrPrevio = (Usuario) obtener(usrActualizado.getIdUsr());
			cambiarEquivalenciaId(usrPrevio, usrActualizado);
			usrPrevio.setNif(usrActualizado.getNif());
			usrPrevio.setNombre(usrActualizado.getNombre());
			usrPrevio.setApellidos(usrActualizado.getApellidos());
			usrPrevio.setDomicilio(usrActualizado.getDomicilio());
			usrPrevio.setCorreo(usrActualizado.getCorreo());
			usrPrevio.setFechaNacimiento(usrActualizado.getFechaNacimiento());
			usrPrevio.setFechaAlta(usrActualizado.getFechaAlta());
			usrPrevio.setRol(usrActualizado.getRol());
			db.store(usrPrevio);
		} 
		catch (DatosException e) {
			throw new DatosException("Actualizar: "+ usrActualizado.getIdUsr() + " no existe.");
		}
		catch (ModeloException e) {
			e.printStackTrace();
		}
	} 

	/**
	 * Obtiene el listado de todos los usuarios almacenados.
	 * @return el texto con el volcado de datos.
	 */
	@Override
	public String listarDatos() {
		return obtenerTodos().toString();
	}

	/**
	 * Obtiene el listado de todos los identificadores de usuario almacenados.
	 * @return el texto con el volcado de datos.
	 */
	@Override
	public String listarId() {
		StringBuilder listado = new StringBuilder();
		for (Usuario usr: obtenerTodos()) {
			if (usr != null) {
				listado.append("\n" + usr.getIdUsr());
			}
		}
		return listado.toString();
	}

	/**
	 * Elimina todos los usuarios almacenados y regenera los predeterminados.
	 */
	@Override
	public void borrarTodo() {
		// Elimina cada uno de los obtenidos
		for (Usuario usr: obtenerTodos()) {
			db.delete(usr);
		}
		// Quita todas las equivalencias
		Map<String,String> mapaEquivalencias = obtenerMapaEquivalencias();
		mapaEquivalencias.clear();
		db.store(mapaEquivalencias);
		cargarPredeterminados();
	}

	//GESTION equivalencias id
	/**
	 * Obtiene el idUsr usado internamente a partir de otro equivalente.
	 * @param id - la clave alternativa. 
	 * @return - El idUsr equivalente.
	 */
	public String obtenerEquivalencia(String id) {
		try {
			ResultSet rsEquival = sentenciaUsr.executeQuery("SELECT * FROM equivalid WHERE equival ="+ id + "");
			if (rsEquival.next()){
				return (String) rsEquival.getObject(2);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Crea las columnas del TableModel a partir de los metadatos del ResulSet
	 * una consulta a base de datos.
	 */
	private void estableceColumnasModelo() {
		try {
			// Obtiene metadatos
			ResultSetMetaData metaDatos = rsUsuarios.getMetaData();
			
			// N�mero total de columnas
			int numCol = metaDatos.getColumnCount();
			
			// Etiqueta de cada columna
			Object[] etiquetas = new Object[numCol];
			for (int i = 0; i < numCol; i++) {
				etiquetas[i] = metaDatos.getColumnLabel(i + 1);
			}
			// Incorpora array de etiquetas en el TableModel
			((DefaultTableModel) tmUsuarios).setColumnIdentifiers(etiquetas);
		   } catch (SQLException e)	{
			   e.printStackTrace();
		}
	}
	/**
	 * Replica en el TableModel las filas del ResultSet
	 */
	private void rellenarFilasModelo() {
		Object[] datosFila = new Object[tmUsuarios.getColumnCount()];
		// Para cada fila en ResulSet de la consulta
		try {
			while (rsUsuarios.next()) {
				// Se replica y a�ade la fila en el TableModel.
				for(int i = 0; i < tmUsuarios.getColumnCount(); i++) {
					datosFila[i] = rsUsuarios.getObject(i + 1);
				}
				((DefaultTableModel) tmUsuarios).addRow(datosFila);
			}
		} catch (SQLException e){
			e.printStackTrace();
		}
		
	}

	/**
	 * Obtiene el mapa de equivalencias de id para idUsr.
	 * @return el Hashtable almacenado.
	 */
	private Map <String, String> obtenerMapaEquivalencias() {
		//Obtiene mapa de equivalencias de id de acceso
		Query consulta = db.query();
		consulta.constrain(Hashtable.class);
		ObjectSet <Hashtable <String,String>> result = consulta.execute();
		return result.get(0);	
	}
	/**
	 * Almacena usuario en la base de datos.
	 * @param usr, el objeto a procesar
	 * @throws SQLException
	 * @throws Datos Exception
	 */
	
	private void almacenar(Usuario usr) throws SQLException {
		ResultSet rsUsr = null;
		//Consulta y los resultados quedane en el ResultSet
		rsUsr = sentenciaUsr.executeQuery("SELECT * FROM usuarios");
		rsUsr.moveToInsertRow();
		rsUsr.updateString("idUsr", usr.getIdUsr());
		rsUsr.updateString("nif", usr.getNif().toString());	
		rsUsr.updateString("nombre", usr.getNombre());
		rsUsr.updateString("apellidos", usr.getApellidos());
		rsUsr.updateString("calle", usr.getDomicilio().getCalle().toString());
		rsUsr.updateString("numero", usr.getDomicilio().getNumero().toString());
		rsUsr.updateString("cp", usr.getDomicilio().getCp().toString());
		rsUsr.updateString("poblacion", usr.getDomicilio().getPoblacion().toString());
		rsUsr.updateString("correo", usr.getCorreo().toString());
		rsUsr.updateDate("fechaNacimiento", new java.sql.Date(usr.getFechaNacimiento().toDate().getTime()));
		rsUsr.updateDate("fechaAlta", new java.sql.Date(usr.getFechaAlta().toDate().getTime()));
		rsUsr.updateString("claveAcceso", usr.getClaveAcceso().toString());
		rsUsr.updateString("rol", usr.getRol().toString());
		rsUsr.insertRow();
		rsUsr.beforeFirst();
		}

	/**
	 * Registra las equivalencias de nif y correo para un idUsr.
	 * @param usuario
	 * @throws SQLException
	 */
	private void registrarEquivalenciaId(Usuario usr) throws SQLException {
		ResultSet rsEquival = null;
		
		rsEquival = sentenciaId.executeQuery("SELECT * FROM equivalid");
		rsEquival.moveToInsertRow();
		rsEquival.updateString("equival", usr.getIdUsr());
		rsEquival.updateString("idUsr", usr.getIdUsr().toString());
		rsEquival.insertRow();
		rsEquival.beforeFirst();
		rsEquival.moveToInsertRow();
		rsEquival.updateString("equival", usr.getNif().toString());
		rsEquival.updateString("idUsr", usr.getIdUsr().toString());
		rsEquival.insertRow();
		rsEquival.beforeFirst();
		rsEquival.moveToInsertRow();
		rsEquival.updateString("equival", usr.getCorreo().toString());
		rsEquival.updateString("idUsr", usr.getIdUsr().toString());
		rsEquival.insertRow();
		rsEquival.beforeFirst();

	}
	/**
	 * Elimina el usuario
	 * @param usr - el usuario para eliminar
	 * @throws SQLExceptio
	 */
	private void borrar(Usuario usr) throws SQLException {
		bufferObjetos.remove(usr);
		sentenciaId.executeQuery("DELETE FROM usuario WHERE idUsr =" + usr.getIdUsr() + "");
	}

	/**
	 * Elimina las equivalencias de nif y correo para un idUsr.
	 * @param string - el usuario para eliminar sus equivalencias de idUsr.
	 */
	private void borrarEquivalenciaId(String string) {
		//Obtiene mapa de equivalencias
		Map<String,String> mapaEquivalencias = obtenerMapaEquivalencias();
		//Borra equivalencias 
		mapaEquivalencias.remove(string.getIdUsr());
		mapaEquivalencias.remove(string.getNif().getTexto());
		mapaEquivalencias.remove(string.getCorreo().getTexto());
		//actualiza datos
		db.store(mapaEquivalencias);	
	}

	/**
	 * Actualiza las equivalencias de nif y correo para un idUsr
	 * @param usrAntiguo - usuario con id's antiguos
	 * @param usrNuevo - usuario con id's nuevos
	 */
	private void cambiarEquivalenciaId(Usuario usrAntiguo, Usuario usrNuevo) {
		//Obtiene mapa de equivalencias
		Map<String,String> mapaEquivalencias = obtenerMapaEquivalencias();
		//Cambia equivalencias 
		mapaEquivalencias.replace(usrAntiguo.getIdUsr(), usrNuevo.getIdUsr().toUpperCase());
		mapaEquivalencias.replace(usrAntiguo.getNif().getTexto(), usrNuevo.getIdUsr().toUpperCase());
		mapaEquivalencias.replace(usrAntiguo.getCorreo().getTexto(), usrNuevo.getIdUsr().toUpperCase());
		//actualiza datos
		db.store(mapaEquivalencias);
	}

} //class