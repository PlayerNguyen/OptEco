<?php
/**
 * APIConfig.php
 *
 * OptEcoWebAPI Project
 *
 * Please read this requirement before use
 * - Compatible with PHP 7 (didn't test on old versions)
 * - Requires the mysqli library: https://www.php.net/manual/en/book.mysqli.php
 *
 * With configuration, you can check the APIConfig.php.
 *
 */

class Config {

    /**
     * Setting your MySQL Server here
     * @var array
     */
    public static $MYSQL = [
        "host"=> "localhost",
        "port"=>"3306",
        "username"=>"root",
        "password"=>"",
        "database"=>"",
        "table"=> "opteco"
    ];

    /**
     * This option depend on what server you are hosting
     */
    public static $ONLINE_MODE = true;

}