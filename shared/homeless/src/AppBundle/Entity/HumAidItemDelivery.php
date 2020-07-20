<?php


namespace AppBundle\Entity;

use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Validator\ExecutionContextInterface;
use Symfony\Component\Validator\Constraints as Assert;

/**
 * Выданная клиенту вещь в пункте выдачи.
 * @package AppBundle\Entity
 * @ORM\Entity()
 */
class HumAidItemDelivery extends BaseEntity
{

    /**
     * @ORM\ManyToOne(targetEntity="Client")
     */
    private $client;

    /**
     * @ORM\ManyToOne(targetEntity="HumAidItem")
     */
    private $humAidItem;

    /**
     * @var \Date
     *
     * @ORM\Column(name="delivered_at", type="date")
     */
    private $deliveredAt;

    /**
     * @return int
     */
    public function getId()
    {
        return $this->id;
    }

    /**
     * @param int $id
     * @return HumAidItemDelivery
     */
    public function setId($id)
    {
        $this->id = $id;
        return $this;
    }

    /**
     * @return mixed
     */
    public function getClient()
    {
        return $this->client;
    }

    /**
     * @param mixed $client
     * @return HumAidItemDelivery
     */
    public function setClient($client)
    {
        $this->client = $client;
        return $this;
    }

    /**
     * @return mixed
     */
    public function getHumAidItem()
    {
        return $this->humAidItem;
    }

    /**
     * @param mixed $humAidItem
     * @return HumAidItemDelivery
     */
    public function setHumAidItem($humAidItem)
    {
        $this->humAidItem = $humAidItem;
        return $this;
    }

    /**
     * @return \DateTime
     */
    public function getDeliveredAt()
    {
        return $this->deliveredAt;
    }

    /**
     * @param \DateTime $deliveredAt
     * @return HumAidItemDelivery
     */
    public function setDeliveredAt($deliveredAt)
    {
        $this->deliveredAt = $deliveredAt;
        return $this;
    }

}