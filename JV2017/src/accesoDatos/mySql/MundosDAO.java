package accesoDatos.mySql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.swing.table.DefaultTableModel;

import modelo.Mundo;
import accesoDatos.DatosException;

public class MundosDAO {

	private static MundosDAO instancia = null;
	private Connection db;

	private Statement sentenciaMn;
	private Statement sentenciaNombre;
	private ResultSet rsMundos;
	private DefaultTableModel tmMundos;
	private ArrayList<Object> bufferObjetos;


	/**
     * Obtiene un mundo dado su nombreMundo.
     * @param nombreMundo - el nombreMundo de Mundo a buscar.
     * @return - el Mundo encontrado.
     * @throws DatosException - si no existe.
     */
	public Mundo obtener(String nombreMundo) throws DatosException {
		try {
			rsMundos = sentenciaMn.executeQuery("SELECT * FROM mundos WHERE nombre = " + nombreMundo + "");
			//Establece columnas de filas.
			estableceColumnasModelo();

			//Borrado previo de filas
			borraFilasModelo();

			//Volcado desde el resulSet
			rellenaFilasModelo();

			//Actualiza buffer de objetos.
			sincronizaBufferObjetos();

			if (bufferObjetos.size() > 0) {
				return(Mundo) bufferObjetos.get(0);
			}
		}
		catch (SQLException e) {
			throw new DatosException("(OBTENER) El mundo: " + nombreMundo + "no existe.");
		}
		return null;
	}


	private void sincronizaBufferObjetos() {
		// TODO Auto-generated method stub
		
	}

	/**
     * Rellena las filas del table-model
     */
	
	private void rellenaFilasModelo() {
		Object[] datosFila = new Object[tmMundos.getColumnCount()];

        try{
            while (rsMundos.next()){
                for (int i=0;i< tmMundos.getColumnCount();i++){
                    datosFila[i] = rsMundos.getObject(i+1);
                }
                ((DefaultTableModel) tmMundos).addRow(datosFila);
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
		
	}


	/**
	 * Borra las filas del table-model
	 */
	private void borraFilasModelo() {
		while (tmMundos.getRowCount() > 0)
			((DefaultTableModel)tmMundos).removeRow(0);;
	} 

	// Crea las columnas del tableModel
	private void estableceColumnasModelo() {
		// TODO Auto-generated method stub
		try {
			ResultSetMetaData metaDatos = rsMundos.getMetaData();

			int numCol = metaDatos.getColumnCount();

			Object[] etiquetas = new Object[numCol];
			for (int i=0; i< numCol; i++){
				etiquetas[i] = metaDatos.getColumnLabel(i +1);
			}
			((DefaultTableModel)tmMundos).setColumnIdentifiers(etiquetas); 

		}catch (SQLException e){

			e.printStackTrace();
		}
	}
	
	/**
     * Borrar un mundo dado un objeto mundo.
     * @param mundo - el objeto de Mundo a borrar.
     * @throws DatosException - si no existe.
     */
	private void borrar(Mundo mundo)throws SQLException{
		bufferObjetos.remove(mundo);
		sentenciaNombre.execute("DELETE FROM mundos where nombre=" + mundo.getNombre() + ";");
	}

}