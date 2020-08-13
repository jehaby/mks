<?php

namespace AppBundle\Entity;

use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Validator\ExecutionContextInterface;
use Symfony\Component\Validator\Constraints as Assert;

/**
 * Вещь в пункте выдачи.
 * @ORM\Entity()
 */
class HumAidItem extends BaseEntity
{

    // Категории
    const CATEGORY_CLOTHES = 3; // одежда
    const CATEGORY_HYGIENE = 17; // гигиена
    const CATEGORY_CRUTCHES = 22; // костыли/трости

    // TODO: отсутствует константа для "костылей и тростей". Оно нам вообще надо?

    /**
     * Название
     * @ORM\Column(type="string", nullable=true)
     */
    private $name;

    /**
     * Категория
     * @ORM\Column(type="integer", nullable=false)
     */
    private $category;

    /**
     * Категория
     * @ORM\Column(type="integer", nullable=false)
     */
    private $limitDays;

    /**
     * Кем создано
     * @ORM\ManyToOne(targetEntity="Application\Sonata\UserBundle\Entity\User", fetch="EXTRA_LAZY")
     */
    protected $createdBy; // TODO EXTRA_LAZY doesn't work as expected in http://localhost/api/v1/clients/1 call

    public function __toString()
    {
        return (string)$this->getName();
    }

    /**
     * Set name
     *
     * @param string $name
     *
     * @return HumAidItem
     */
    public function setName($name)
    {
        $this->name = $name;

        return $this;
    }

    /**
     * Get name
     *
     * @return string
     */
    public function getName()
    {
        return $this->name;
    }

    /**
     * Set category
     *
     * @param integer $category
     *
     * @return HumAidItem
     */
    public function setCategory($category)
    {
        $this->category = $category;

        return $this;
    }

    /**
     * Get category
     *
     * @return integer
     */
    public function getCategory()
    {
        return $this->category;
    }

    /**
     * Set limitDays
     *
     * @param integer $limitDays
     *
     * @return HumAidItem
     */
    public function setLimitDays($limitDays)
    {
        $this->limitDays = $limitDays;

        return $this;
    }

    /**
     * Get limitDays
     *
     * @return integer
     */
    public function getLimitDays()
    {
        return $this->limitDays;
    }

    /**
     * @Assert\Callback
     */
    public function validate(ExecutionContextInterface $context)
    {
        if ($this->getLimitDays() < 0) {
            $context->addViolationAt(
                'limitDays',
                'Не может быть отрицательным',
                [],
                null
            );
        }
    }

}
