from mysql import connector


class DataStore2:
    """
    This is an example of data access controller to use in DAO classes generated by SQL DAL Maker.
    Web-site: http://sqldalmaker.sourceforge.net
    Contact: sqldalmaker@gmail.com
    Copy-paste this code to your project and change it for your needs.
    """

    def __init__(self):
        self._con = None

    def open(self):
        self._con = connector.Connect(user='root', password='root',
                                      host='127.0.0.1',
                                      database='orders')

    def close(self):
        if self._con:
            self._con.close()

    def start_transaction(self):
        self._con.start_transaction()

    def commit(self):
        self._con.commit()

    def rollback(self):
        self._con.rollback()

    @staticmethod
    def _prepare_sql(sql):
        """
        @rtype : str
        """
        return sql.replace("?", "%s")

    def insert_row(self, sql, params, ai_values):
        """
        Returns:
            Nothing.
        Arguments:
            param1 (string): SQL statement.
            param2 (array, optional): Values of SQL parameters.
            param2 (array, optional): Array like [["o_id", 1], ...] for auto-increment values.
        Raises:
            Exception: if no rows inserted.
        """
        sql = self._prepare_sql(sql)
        cur = self._con.cursor()
        cur.execute(sql, params)
        if len(ai_values) > 0:
            ai_values[0][1] = cur.lastrowid
        if cur.rowcount == 0:
            raise Exception('No rows inserted')

    def exec_dml(self, sql, params):
        """
        Arguments:
            param1 (string): SQL statement.
            param2 (array, optional): Values of SQL parameters.
        Returns:
            Number of updated rows.
        """
        sql = self._prepare_sql(sql)
        cur = self._con.cursor()
        cur.execute(sql, params)
        return cur.rowcount

    def query_scalar(self, sql, params):
        """
        Returns:
            Single scalar value.
        Arguments:
            param1 (string): SQL statement.
            param2 (array, optional): Values of SQL parameters if needed.
        Raises:
            Exception: if amount of rows != 1.
        """
        sql = self._prepare_sql(sql)

        rows = self.query_scalar_array(sql, params)
        if len(rows) == 0:
            raise Exception('No rows')
        if len(rows) > 1:
            raise Exception('More than 1 row exists')
        return rows[0][0]

    def query_scalar_array(self, sql, params):
        """
        Returns:
            array of scalar values
        Arguments:
            param1 (string): SQL statement.
            param2 (array, optional): Values of SQL parameters if needed.
        """
        sql = self._prepare_sql(sql)
        res = []
        cursor = self._con.cursor()
        cursor.execute(sql, params)
        row = cursor.fetchone()
        while row is not None:
            res.append(row[0])
            row = cursor.fetchone()
        return res

    def query_single_row(self, sql, params):
        """
        Returns:
            Single row
        Arguments:
            param1 (string): SQL statement.
            param2 (array, optional): Values of SQL parameters if needed.
        Raises:
            Exception: if amount of rows != 1.
        """
        sql = self._prepare_sql(sql)

        rows = []

        def callback(row):
            rows.append(row)

        self.query_all_rows(sql, params, callback)
        if len(rows) == 0:
            raise Exception('No rows')
        if len(rows) > 1:
            raise Exception('More than 1 row exists')
        return rows[0]

    def query_all_rows(self, sql, params, callback):
        """
        Returns:
            None
        Arguments:
            param1 (string): SQL statement.
            param2 (array, optional): Values of SQL parameters if needed.
        """
        sql = self._prepare_sql(sql)
        # http://geert.vanderkelen.org/connectorpython-custom-cursors/
        # Fetching rows as dictionaries with MySQL Connector/Python
        cursor = self._con.cursor(cursor_class=MySQLCursorDict)
        cursor.execute(sql, params)
        row = cursor.fetchone()
        while row is not None:
            callback(row)
            row = cursor.fetchone()

class MySQLCursorDict(connector.cursor.MySQLCursor):
    def _row_to_python(self, row_data, desc=None):
        row = super(MySQLCursorDict, self)._row_to_python(row_data, desc)
        if row:
            return dict(zip(self.column_names, row))
        return None