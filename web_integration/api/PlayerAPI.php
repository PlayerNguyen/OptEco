<?php
/**
 * PlayerAPI.php
 *
 * OptEcoWebAPI Project
 *
 * Please read this requirement before use
 * - Compatible with PHP 7 (didn't test on old versions)
 * - Requires the mysqli library: https://www.php.net/manual/en/book.mysqli.php
 *
 * With configuration, you can check the APIConfig.php.
 */

require_once "APIConfig.php";
require_once "IMojangAPI.php";
require_once "MojangAPI.php";
class PlayerAPI
{

    private static $UUID_OFFLINE_FORMAT = "OfflinePlayer:";

    private $username;

    public function __construct($username)
    {
        $this->username = $username;
    }

    /**
     * Get the UUID
     * @return bool|string
     * @since 1.0
     */
    public function getUUID() {
        if (Config::$ONLINE_MODE) {
            return MojangAPI::formatUuid(MojangAPI::getUuid($this->username));
        } else {
            $data = hex2bin(md5(self::$UUID_OFFLINE_FORMAT . $this->username));
            $data[6] = chr(ord($data[6]) & 0x0f | 0x30);
            $data[8] = chr(ord($data[8]) & 0x3f | 0x80);
            return self::generateUUID(bin2hex($data));
        }
    }

    /**
     * Generate uuid for offline
     * @param $striped
     * @param bool $minimal
     * @return string
     * @since 1.0
     */
    protected function generateUUID($striped, $minimal = false) {
        $components = array(
            substr($striped, 0, 8),
            substr($striped, 8, 4),
            substr($striped, 12, 4),
            substr($striped, 16, 4),
            substr($striped, 20),
        );
        if ($minimal) return implode('', $components);
        return implode('-', $components);
    }

}